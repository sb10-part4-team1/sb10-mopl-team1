package com.sb10.mopl.batch.scheduler;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
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
 *
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
  private final Job sportsJob;
  private final Job tmdbJob;

  /** 10분마다 실패한 스포츠 배치를 자동 복구합니다. (KST 02:00~07:00 시간대만 동작) */
  @Scheduled(fixedDelay = 600000)
  public void recoverSportsJob() {
    if (isRecoveryUnavailableTime()) { // KST 02:00~07:00 체크
      return;
    }
    recoverJob("sportsJob", sportsJob, "[RECOVERY-SPORTS]");
  }

  /** 10분마다 실패한 TMDB 배치를 자동 복구합니다. (KST 02:00~07:00 시간대만 동작) */
  @Scheduled(fixedDelay = 600000)
  public void recoverTmdbJob() {
    if (isRecoveryUnavailableTime()) {
      return;
    }
    recoverJob("tmdbJob", tmdbJob, "[RECOVERY-TMDB]");
  }

  /** 공통 복구 실행 메서드 */
  private void recoverJob(String jobName, Job job, String logPrefix) {
    log.debug("{} 실패 배치 모니터링 시작", logPrefix);

    // 1. 중복 기동 방지: 해당 Job이 현재 이미 실행 중(STARTED)인지 먼저 확인합니다.
    Set<JobExecution> runningExecutions = jobExplorer.findRunningJobExecutions(jobName);
    if (!runningExecutions.isEmpty()) {
      log.info("{} {} 배치 실행 중으로 중복 기동 방지 스킵", logPrefix, jobName);
      return;
    }

    // 2. 해당 Job의 가장 최근 실행 인스턴스 1건만 가져옵니다.
    List<JobInstance> instances = jobExplorer.getJobInstances(jobName, 0, 1);
    if (instances.isEmpty()) {
      return;
    }

    JobInstance latestInstance = instances.get(0);
    List<JobExecution> executions = jobExplorer.getJobExecutions(latestInstance);
    if (executions.isEmpty()) {
      return;
    }

    // 중요 변수 명시적 추출
    JobExecution lastExecution = executions.get(0); // 가장 최근 실행 1건 (최신순 정렬)
    BatchStatus lastStatus = lastExecution.getStatus(); // 최근 실행의 상태 (COMPLETED, FAILED, STARTED 등)

    // 3. 가장 최근 실행 상태가 FAILED일 때만 복구 대상입니다.
    if (lastStatus == BatchStatus.FAILED) {

      // 4. 재시도 가능 여부 화이트리스트 검사 - 치명적 에러면 즉시 관리자 알림 후 락 처리합니다.
      if (!isRetryableFailure(lastExecution)) {
        log.error("{} {} 배치 치명적 에러 감지로 자동 재시작 차단", logPrefix, jobName);
        notifyAdminWithDetails(lastExecution, logPrefix, "치명적 에러 감지 (재시도 불가)");
        return;
      }

      // 동일 JobInstance 내에서 누적된 실패(FAILED) 횟수를 구합니다.
      long failedCount =
          executions.stream().filter(exec -> exec.getStatus() == BatchStatus.FAILED).count();

      // 임계 차단: 동일 인스턴스 실패 횟수가 3회 이상이면 영구 중단(Lock) 후 관리자 알림
      if (failedCount >= 3) {
        log.error("{} {} 배치 3회 연속 복구 실패로 자동 재시작 중단 및 락 처리", logPrefix, jobName);
        notifyAdminWithDetails(lastExecution, logPrefix, "3회 연속 복구 실패");
        return;
      }

      // 5. 3회 미만일 때는 실패했던 파라미터 그대로 이어서 재시작(Restart)을 트리거합니다.
      log.warn("{} {} 배치 실패 감지로 10분 쿨다운 후 재시작 진행 (누적 실패: {}/3)", logPrefix, jobName, failedCount);
      try {
        jobLauncher.run(job, lastExecution.getJobParameters());
      } catch (Exception e) {
        log.error("{} {} 배치 자동 재시작 실패: {}", logPrefix, jobName, e.getMessage());
      }
    }
  }

  /*
   * 재시도 가능 예외 화이트리스트 검사 메서드.
   * 일시적인 통신 장애(타임아웃, 5xx, 429)에 의한 실패만 재시도를 허용합니다.
   * 그 외 모든 예외(인증 오류, 데이터 오류, 경로 유실 등)는 치명적 에러로 간주하여 즉시 관리자에게 알립니다.
   */
  private boolean isRetryableFailure(JobExecution execution) {
    List<Throwable> exceptions = execution.getAllFailureExceptions();
    if (exceptions.isEmpty()) {
      return false;
    }
    return exceptions.stream()
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
   * 복구 허용 시간대 판별 메서드.
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
