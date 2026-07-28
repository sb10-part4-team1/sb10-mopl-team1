package com.sb10.mopl.batch.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sb10.mopl.batch.service.BatchAdminService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.web.client.ResourceAccessException;

@ExtendWith(MockitoExtension.class)
class BatchAutoRecoverySchedulerTest {

  @Mock private JobLauncher jobLauncher;
  @Mock private JobExplorer jobExplorer;
  @Mock private BatchAdminService batchAdminService;
  @Mock private Job sportsJob;
  @Mock private Job tmdbJob;

  private MeterRegistry meterRegistry;
  private BatchAutoRecoveryScheduler recoveryScheduler;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    recoveryScheduler =
        spy(
            new BatchAutoRecoveryScheduler(
                jobLauncher, jobExplorer, batchAdminService, sportsJob, tmdbJob, meterRegistry));
  }

  @Nested
  @DisplayName("1. 복구 시간대(isRecoveryUnavailableTime) 판별 검증")
  class TimeWindowTests {

    @Test
    @DisplayName("KST 02시~06시 사이일 때 복구 허용(false) 상태를 반환한다")
    void isRecoveryUnavailableTime_returnsFalse_duringAllowedHours() {
      // given: KST 03시 시각 준비
      ZonedDateTime allowedTime =
          ZonedDateTime.of(2026, 7, 27, 3, 0, 0, 0, ZoneId.of("Asia/Seoul"));

      // when: 시간 검사 수행
      boolean unavailable = recoveryScheduler.isRecoveryUnavailableTime(allowedTime);

      // that: 복구가 가능(false)한지 검증한다
      assertThat(unavailable).isFalse();
    }

    @Test
    @DisplayName("KST 07시~01시 사이일 때 복구 불허(true) 상태를 반환하여 복구를 차단한다")
    void isRecoveryUnavailableTime_returnsTrue_duringDisallowedHours() {
      // given: KST 08시 및 KST 01시 준비
      ZonedDateTime morningTime =
          ZonedDateTime.of(2026, 7, 27, 8, 0, 0, 0, ZoneId.of("Asia/Seoul"));
      ZonedDateTime nightTime = ZonedDateTime.of(2026, 7, 27, 1, 30, 0, 0, ZoneId.of("Asia/Seoul"));

      // when & that: 복구 불허(true)인지 검증한다
      assertThat(recoveryScheduler.isRecoveryUnavailableTime(morningTime)).isTrue();
      assertThat(recoveryScheduler.isRecoveryUnavailableTime(nightTime)).isTrue();
    }

    @Test
    @DisplayName("KST 02시 정각(하한 경계값)일 때 복구 허용(false) 상태를 반환한다")
    void isRecoveryUnavailableTime_returnsFalse_atLowerBoundary() {
      // given: KST 02시 정각 시각 준비
      ZonedDateTime boundaryTime =
          ZonedDateTime.of(2026, 7, 27, 2, 0, 0, 0, ZoneId.of("Asia/Seoul"));

      // when: 시간 검사 수행
      boolean unavailable = recoveryScheduler.isRecoveryUnavailableTime(boundaryTime);

      // that: 복구가 가능(false)한지 검증한다
      assertThat(unavailable).isFalse();
    }

    @Test
    @DisplayName("KST 07시 정각(상한 경계값)일 때 복구 불허(true) 상태를 반환한다")
    void isRecoveryUnavailableTime_returnsTrue_atUpperBoundary() {
      // given: KST 07시 정각 시각 준비
      ZonedDateTime boundaryTime =
          ZonedDateTime.of(2026, 7, 27, 7, 0, 0, 0, ZoneId.of("Asia/Seoul"));

      // when: 시간 검사 수행
      boolean unavailable = recoveryScheduler.isRecoveryUnavailableTime(boundaryTime);

      // that: 복구가 불허(true)되는지 검증한다
      assertThat(unavailable).isTrue();
    }
  }

  @Nested
  @DisplayName("2. BatchAutoRecoveryScheduler 자동 복구 비즈니스 규칙 검증")
  class RecoveryRuleTests {

    @BeforeEach
    void overrideTimeCheck() {
      // 시간 체크 시 허용 시간대(false)를 반환하도록 spy 오버라이딩
      when(recoveryScheduler.isRecoveryUnavailableTime()).thenReturn(false);
    }

    @Test
    @DisplayName("복구 대상(FAILED 인스턴스)이 없거나 null인 경우 아무 작업도 수행하지 않고 스킵한다")
    void recoverSportsJob_skips_whenNoFailedExecutionExists() throws Exception {
      // given: 복구 대상이 없음(null)을 설정한다
      when(batchAdminService.validateAndGetLastFailedExecution("sportsJob")).thenReturn(null);

      // when: recoverSportsJob 실행 시
      recoveryScheduler.recoverSportsJob();

      // that: jobLauncher.run이 호출되지 않고 스킵되는지 검증한다
      verify(jobLauncher, never()).run(any(Job.class), any(JobParameters.class));
    }

    @Test
    @DisplayName("치명적 에러(재시도 불가능한 예외) 감지 시 자동으로 재시작을 차단한다")
    void recoverSportsJob_blocks_whenFatalFailureDetected() throws Exception {
      // given: FAILED 인스턴스 및 치명적 에러(IllegalArgumentException) 발생 이력 설정
      JobInstance instance = new JobInstance(1L, "sportsJob");
      JobExecution failedExecution = new JobExecution(instance, 10L, null);
      failedExecution.setStatus(BatchStatus.FAILED);
      failedExecution.addFailureException(new IllegalArgumentException("잘못된 파라미터"));

      when(batchAdminService.validateAndGetLastFailedExecution("sportsJob"))
          .thenReturn(failedExecution);

      // when: recoverSportsJob 실행 시
      recoveryScheduler.recoverSportsJob();

      // that: 치명적 에러로 판단하여 jobLauncher.run이 호출되지 않고 차단되는지 검증한다
      verify(jobLauncher, never()).run(any(Job.class), any(JobParameters.class));
    }

    @Test
    @DisplayName("동일 JobInstance의 누적 실패 횟수가 3회 이상일 때 자동 재시작을 차단한다")
    void recoverSportsJob_blocks_whenFailedCountExceedsThree() throws Exception {
      // given: 일시적 예외(ResourceAccessException) 발생 이력이 3회 이상인 인스턴스 구성
      JobInstance instance = new JobInstance(1L, "sportsJob");
      JobExecution exec1 = new JobExecution(instance, 10L, null);
      exec1.setStatus(BatchStatus.FAILED);
      exec1.addFailureException(new ResourceAccessException("타임아웃"));

      JobExecution exec2 = new JobExecution(instance, 11L, null);
      exec2.setStatus(BatchStatus.FAILED);
      exec2.addFailureException(new ResourceAccessException("타임아웃"));

      JobExecution exec3 = new JobExecution(instance, 12L, null);
      exec3.setStatus(BatchStatus.FAILED);
      exec3.addFailureException(new ResourceAccessException("타임아웃"));

      when(batchAdminService.validateAndGetLastFailedExecution("sportsJob")).thenReturn(exec3);
      when(jobExplorer.getJobExecutions(instance)).thenReturn(List.of(exec1, exec2, exec3));

      // when: recoverSportsJob 실행 시
      recoveryScheduler.recoverSportsJob();

      // that: 실패 횟수 3회 도달로 인해 jobLauncher.run이 호출되지 않는지 검증한다
      verify(jobLauncher, never()).run(any(Job.class), any(JobParameters.class));
    }

    @Test
    @DisplayName("일시적 네트워크 장애로 1회 실패하였고 실패 횟수가 3회 미만인 경우 실패 지점부터 재시작을 수행한다")
    void recoverSportsJob_restartsJob_whenFailedCountLessThanThree() throws Exception {
      // given: 일시적 네트워크 타임아웃 예외(ResourceAccessException)로 1회 실패한 이력 설정
      JobInstance instance = new JobInstance(1L, "sportsJob");
      JobExecution exec1 = new JobExecution(instance, 10L, null);
      exec1.setStatus(BatchStatus.FAILED);
      exec1.addFailureException(new ResourceAccessException("네트워크 타임아웃"));

      when(batchAdminService.validateAndGetLastFailedExecution("sportsJob")).thenReturn(exec1);
      when(jobExplorer.getJobExecutions(instance)).thenReturn(List.of(exec1));

      // when: recoverSportsJob 실행 시
      recoveryScheduler.recoverSportsJob();

      // that: jobLauncher.run이 sportsJob과 함께 정상 트리거되는지 검증한다
      verify(jobLauncher).run(eq(sportsJob), any(JobParameters.class));
    }
  }
}
