package com.sb10.mopl.batch.listener;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.stereotype.Component;

/*
 * 배치 잡(Job)과 스텝(Step)의 실행 수명주기 동안 메트릭을 수집하는 통합 리스너입니다.
 *
 * 1. Job 완료 시: 성공(1.0) / 실패(0.0) 상태 게이지 및 소요 시간 타이머를 갱신합니다.
 * 2. Step 완료 시: 읽기, 쓰기, 필터, 스킵 건수 카운터를 누적 합산합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MoplBatchMetricsListener implements JobExecutionListener, StepExecutionListener {

  private final MeterRegistry meterRegistry;

  // 잡별 최신 완료 상태값 보관 맵 (1.0 = COMPLETED, 0.0 = FAILED/기타)
  private final Map<String, Double> jobStatuses = new ConcurrentHashMap<>();

  @Override
  public void beforeJob(JobExecution jobExecution) {
    String jobName = jobExecution.getJobInstance().getJobName();
    // 시작 시점에는 진행 중이므로 일단 FAILED/STARTED 상태(0.0)로 세팅
    jobStatuses.put(jobName, 0.0);

    // 게이지(Gauge) 동적 등록 및 모니터링 바인딩
    meterRegistry.gauge(
        "mopl.batch.job.status",
        List.of(Tag.of("jobName", jobName)),
        jobStatuses,
        map -> map.getOrDefault(jobName, 0.0));
  }

  @Override
  public void afterJob(JobExecution jobExecution) {
    String jobName = jobExecution.getJobInstance().getJobName();
    BatchStatus batchStatus = jobExecution.getStatus();

    // 1. 게이지 상태값 업데이트 (COMPLETED 성공 시 1.0, 그 외 롤백/실패 시 0.0)
    double finalStatus = (batchStatus == BatchStatus.COMPLETED) ? 1.0 : 0.0;
    jobStatuses.put(jobName, finalStatus);

    // 2. 잡 실행 소요 시간(Duration) 기록
    LocalDateTime startTime = jobExecution.getStartTime();
    LocalDateTime endTime = jobExecution.getEndTime();

    if (startTime != null && endTime != null) {
      long durationMs = Duration.between(startTime, endTime).toMillis();
      Timer.builder("mopl.batch.job.duration.seconds")
          .description("Duration of batch job execution")
          .tags("jobName", jobName, "status", batchStatus.name())
          .register(meterRegistry)
          .record(durationMs, TimeUnit.MILLISECONDS);

      log.info(
          "배치 잡 메트릭 갱신 완료 - Job: {}, Status: {}, Duration: {}ms", jobName, batchStatus, durationMs);
    }
  }

  @Override
  public void beforeStep(StepExecution stepExecution) {
    // 스텝 시작 시점에는 별도의 지표를 초기화할 필요가 없음
  }

  @Override
  public ExitStatus afterStep(StepExecution stepExecution) {
    String stepName = stepExecution.getStepName();

    // 1. Read Count 누적
    Counter.builder("mopl.batch.step.items.total")
        .description("Total processed items in batch step")
        .tags("stepName", stepName, "type", "read")
        .register(meterRegistry)
        .increment(stepExecution.getReadCount());

    // 2. Write Count 누적
    Counter.builder("mopl.batch.step.items.total")
        .description("Total processed items in batch step")
        .tags("stepName", stepName, "type", "write")
        .register(meterRegistry)
        .increment(stepExecution.getWriteCount());

    // 3. Filter Count 누적
    Counter.builder("mopl.batch.step.items.total")
        .description("Total processed items in batch step")
        .tags("stepName", stepName, "type", "filter")
        .register(meterRegistry)
        .increment(stepExecution.getFilterCount());

    // 4. Skip Count 누적 (Read + Process + Write Skip 합산)
    long totalSkipCount =
        stepExecution.getReadSkipCount()
            + stepExecution.getProcessSkipCount()
            + stepExecution.getWriteSkipCount();
    Counter.builder("mopl.batch.step.items.total")
        .description("Total processed items in batch step")
        .tags("stepName", stepName, "type", "skip")
        .register(meterRegistry)
        .increment(totalSkipCount);

    log.info(
        "배치 스텝 메트릭 누적 완료 - Step: {}, Read: {}, Write: {}, Filter: {}, Skip: {}",
        stepName,
        stepExecution.getReadCount(),
        stepExecution.getWriteCount(),
        stepExecution.getFilterCount(),
        totalSkipCount);

    return null; // StepExecutionListener 규칙에 따라 null 반환 시 기존 ExitStatus 유지
  }
}
