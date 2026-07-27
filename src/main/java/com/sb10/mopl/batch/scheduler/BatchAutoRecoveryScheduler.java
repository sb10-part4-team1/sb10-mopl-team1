package com.sb10.mopl.batch.scheduler;

import com.sb10.mopl.batch.exception.BatchException;
import com.sb10.mopl.batch.service.BatchAdminService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import jakarta.annotation.PostConstruct;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

/*
 * FAILED 상태로 멈춘 배치 작업을 감지하여 10분마다 자동으로 이어서 재시작(Restart)을 수행하는 통합 복구 스케줄러입니다.
 * [스프링 배치 구조 및 자동 초기화 원리]
 * 1. JobInstance (논리적 배치 단위): Job 이름 + JobParameters 조합이 같으면 동일한 인스턴스로 취급됩니다.
 *    - 매일 새벽 1시에 도는 정기 배치는 time 파라미터(System.currentTimeMillis())가 달라지므로 새로운 JobInstance ID가 발급됩니다.
 *    - 따라서 날짜가 바뀌어 새 배치가 돌기 시작하면 자동으로 이전 락(Lock)에서 벗어나 새로운 복구 주기가 발동합니다.
 * 2. JobExecution (실제 실행 단위): 하나의 JobInstance 하위에 여러 개의 실행(Execution)이 쌓입니다.
 *    - 재시작(Restart)은 새로운 파라미터를 만드는 것이 아니라, 실패한 동일 JobInstance 하위에 새로운 JobExecution을 추가하는 것입니다.
 * 3. 복구 스캔 한도 (최신 1건): 과거 락이 걸린 배치가 복구 로직에 스캔되지 않도록 합니다.,
 *    각 Job별로 가장 최신의 JobInstance 딱 1건만 조회하여 모니터링합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BatchAutoRecoveryScheduler {

  private final JobLauncher jobLauncher;
  private final JobExplorer jobExplorer;
  private final BatchAdminService batchAdminService;
  private final Job sportsJob;
  private final Job tmdbJob;
  private final MeterRegistry meterRegistry;

  /* 연속 실패 상태 보관을 위한 실시간 맵 */
  private final Map<String, Double> consecutiveFailures = new ConcurrentHashMap<>();

  // 이미 락이 걸리거나 치명적 에러로 알림을 보낸 JobExecution ID 목록을 기억하여 중복 스팸을 차단합니다.
  private final Set<Long> notifiedExecutionIds = ConcurrentHashMap.newKeySet();

  /*
   * 애플리케이션 시작 시 모니터링할 배치 작업들의 연속 실패 게이지를 사전 등록합니다.
   */
  @PostConstruct
  public void registerRecoveryFailureGauges() {
    registerConsecutiveFailureGauge("sportsJob");
    registerConsecutiveFailureGauge("tmdbJob");
  }

  private void registerConsecutiveFailureGauge(String jobName) {
    consecutiveFailures.putIfAbsent(jobName, 0.0);
    meterRegistry.gauge(
        "mopl.batch.recovery.failure.consecutive",
        List.of(Tag.of("jobName", jobName)),
        consecutiveFailures,
        map -> map.getOrDefault(jobName, 0.0));
  }

  /* 10분마다 실패한 스포츠 배치를 자동 복구합니다. (KST 02:00~07:00 시간대만 동작) */
  @Scheduled(fixedDelay = 600000)
  @SchedulerLock(name = "recoverSportsJobLock", lockAtMostFor = "9m", lockAtLeastFor = "1m")
  public void recoverSportsJob() {
    recoverJob("sportsJob", sportsJob, "[RECOVERY-SPORTS]");
  }

  /* 10분마다 실패한 TMDB 배치를 자동 복구합니다. (KST 02:00~07:00 시간대만 동작) */
  @Scheduled(fixedDelay = 600000)
  @SchedulerLock(name = "recoverTmdbJobLock", lockAtMostFor = "9m", lockAtLeastFor = "1m")
  public void recoverTmdbJob() {
    recoverJob("tmdbJob", tmdbJob, "[RECOVERY-TMDB]");
  }

  /** 공통 복구 실행 메서드 */
  private void recoverJob(String jobName, Job job, String logPrefix) {
    if (isRecoveryUnavailableTime()) { // KST 02:00~07:00 체크
      return;
    }

    log.debug("{} 실패 배치 모니터링 시작", logPrefix);

    try {
      // 1. 중복 기동 방지 및 최근 실행 인스턴스의 FAILED 상태 조회 (서비스 공통 검증 위임)
      JobExecution lastExecution = batchAdminService.validateAndGetLastFailedExecution(jobName);
      if (lastExecution == null) {
        consecutiveFailures.put(jobName, 0.0); // 실패 건이 없으므로 연속 실패 리셋
        return; // 복구 대상(FAILED)이 없거나 실행 이력이 없음
      }

      // 2. 이미 3회 실패 락/치명적 에러로 중단되고 알림이 나갔던 실행 건인지 검증
      if (notifiedExecutionIds.contains(lastExecution.getId())) {
        return;
      }

      // 2. 치명적 에러 감지 시 즉시 락 처리 및 관리자 알림 발송
      if (isFatalFailure(lastExecution)) {
        log.error("{} {} 배치 치명적 에러 감지로 자동 재시작 차단", logPrefix, jobName);
        consecutiveFailures.put(jobName, 3.0); // 치명적 실패는 즉시 최종 잠금값인 3.0 설정
        notifyAdminWithDetails(lastExecution, logPrefix, "치명적 에러 감지 (재시도 불가)");
        notifiedExecutionIds.add(lastExecution.getId()); // 알림 발송 기록
        return;
      }

      // 3. 동일 JobInstance 내에서 누적된 실패(FAILED) 횟수를 구하기 위해 전체 이력 조회
      List<JobExecution> executions = jobExplorer.getJobExecutions(lastExecution.getJobInstance());
      long failedCount =
          executions.stream().filter(exec -> exec.getStatus() == BatchStatus.FAILED).count();

      consecutiveFailures.put(jobName, (double) failedCount); // 실시간 연속 실패 횟수 갱신

      // 임계 차단: 동일 인스턴스 실패 횟수가 3회 이상이면 영구 중단(Lock) 후 관리자 알림
      if (failedCount >= 3) {
        log.error("{} {} 배치 3회 연속 복구 실패로 자동 재시작 중단 및 락 처리", logPrefix, jobName);
        notifyAdminWithDetails(lastExecution, logPrefix, "3회 연속 복구 실패");
        notifiedExecutionIds.add(lastExecution.getId()); // 알림 발송 기록
        return;
      }

      // 4. 3회 미만일 때는 실패했던 파라미터 그대로 이어서 재시작(Restart)을 트리거합니다.
      log.warn("{} {} 배치 실패 감지로 10분 쿨다운 후 재시작 진행 (누적 실패: {}/3)", logPrefix, jobName, failedCount);

      /* 복구 재기동 시도 횟수 카운터 증가 */
      Counter.builder("mopl.batch.recovery.attempts.total")
          .description("Total recovery scheduler attempts")
          .tags("jobName", jobName)
          .register(meterRegistry)
          .increment();

      jobLauncher.run(job, lastExecution.getJobParameters());

    } catch (BatchException e) {
      // 구조적으로 오직 JOB_ALREADY_RUNNING(중복 기동) 예외만 유입되므로 즉시 스킵 로깅
      log.info("{} {} 배치 검증 실패로 스킵: {}", logPrefix, jobName, e.getMessage());
    } catch (Exception e) {
      // 그 외의 모든 스프링 배치 기동 예외 및 일반 예외 처리
      log.error("{} {} 배치 자동 재시작 과정에서 예외 발생: {}", logPrefix, jobName, e.getMessage());
    }
  }

  /*
   * 치명적 에러(재시도 불가) 감지 메서드.
   * 모든 예외가 일시적인 통신 장애(타임아웃, 5xx, 429)일 경우에만 false(재시도 가능)를 반환하고,
   * 그 외 모든 예외(인증 오류, 데이터 오류 등)는 true(치명적 에러)를 반환합니다.
   */
  private boolean isFatalFailure(JobExecution execution) {
    List<Throwable> exceptions = execution.getAllFailureExceptions();
    if (exceptions.isEmpty()) {
      return false; // 예외가 발생한 적 없으면 false
    }
    return !exceptions.stream() // 아래의 Exception과 일치하면 false
        .allMatch(
            e ->
                e instanceof ResourceAccessException
                    || e instanceof HttpServerErrorException
                    || e instanceof HttpClientErrorException.TooManyRequests);
  }

  /*
   * 관리자 알림 상세 로그 발행 메서드.
   * 실패한 Job의 Step별 처리 건수, 예외 유형, 예외 메시지를 상세하게 로깅합니다.
   * TODO: [Kafka] 아래 정보를 카프카 이벤트로 발행하여 디스코드 웹훅으로 관리자 알림 발송 예정
   */
  private void notifyAdminWithDetails(JobExecution execution, String logPrefix, String reason) {
    log.error(
        "[ADMIN-ALERT] {} 배치 복구 최종 실패 - 원인: {}", execution.getJobInstance().getJobName(), reason);

    for (StepExecution step : execution.getStepExecutions()) {
      if (step.getStatus() == BatchStatus.FAILED) {
        log.error(
            "[ADMIN-ALERT] 실패 Step: {}, 읽기: {}건, 쓰기: {}건, 스킵: {}건",
            step.getStepName(),
            step.getReadCount(),
            step.getWriteCount(),
            step.getSkipCount());
        step.getFailureExceptions()
            .forEach(
                e ->
                    log.error(
                        "[ADMIN-ALERT] 예외 유형: {}, 메시지: {}",
                        e.getClass().getSimpleName(),
                        e.getMessage()));
      }
    }
  }

  /*
   * 복구 불가 시간대 판별 메서드.
   * KST 01:00에 실행되는 정기 배치가 실패했을 때, KST 02:00부터 07:00 사이에만 복구를 허용합니다.
   * 07:00 이후에는 TMDB 데이터 갱신(KST 09:00) 전 안전 버퍼를 위해 복구를 차단합니다.
   */
  private boolean isRecoveryUnavailableTime() {
    int hour = ZonedDateTime.now(ZoneId.of("Asia/Seoul")).getHour();
    if (hour < 2 || hour >= 7) {
      log.debug("[RECOVERY] 복구 허용 시간대(KST 02:00~07:00) 외로 스케줄러 스킵 - 현재 KST {}:xx", hour);
      return true;
    }
    return false;
  }
}
