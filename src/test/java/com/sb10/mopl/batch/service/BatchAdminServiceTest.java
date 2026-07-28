package com.sb10.mopl.batch.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sb10.mopl.batch.exception.BatchErrorCode;
import com.sb10.mopl.batch.exception.BatchException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
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

@ExtendWith(MockitoExtension.class)
class BatchAdminServiceTest {

  @Mock private JobLauncher jobLauncher;
  @Mock private JobExplorer jobExplorer;
  @Mock private Job sportsJob;
  @Mock private Job tmdbJob;

  private BatchAdminService batchAdminService;

  @BeforeEach
  void setUp() {
    batchAdminService = new BatchAdminService(jobLauncher, jobExplorer, sportsJob, tmdbJob);
  }

  @Nested
  @DisplayName("1. validateAndGetLastFailedExecution 검증")
  class ValidateAndGetLastFailedExecutionTests {

    @Test
    @DisplayName("이미 실행 중(STARTED)인 배치 Job에 대해 검증 시 JOB_ALREADY_RUNNING 예외가 발생한다")
    void validateAndGetLastFailedExecution_fail_whenAlreadyRunning() {
      // given: sportsJob이 이미 실행 중인 스터빙 설정
      JobExecution runningExecution = new JobExecution(1L);
      when(jobExplorer.findRunningJobExecutions("sportsJob")).thenReturn(Set.of(runningExecution));

      // when & that: 메서드 실행 시 BatchException(JOB_ALREADY_RUNNING) 예외가 던져지는지 검증한다
      assertThatThrownBy(() -> batchAdminService.validateAndGetLastFailedExecution("sportsJob"))
          .isInstanceOf(BatchException.class)
          .extracting("errorCode")
          .isEqualTo(BatchErrorCode.JOB_ALREADY_RUNNING);
    }

    @Test
    @DisplayName("실행 이력이 없는 경우 null을 반환한다")
    void validateAndGetLastFailedExecution_returnsNull_whenNoJobInstancesExist() {
      // given: 실행 중인 배치가 없고 인스턴스 이력도 없는 상황 설정
      when(jobExplorer.findRunningJobExecutions("sportsJob")).thenReturn(Collections.emptySet());
      when(jobExplorer.getJobInstances("sportsJob", 0, 1)).thenReturn(Collections.emptyList());

      // when: 메서드를 호출할 때
      JobExecution result = batchAdminService.validateAndGetLastFailedExecution("sportsJob");

      // that: 결과가 null인지 검증한다
      assertThat(result).isNull();
    }

    @Test
    @DisplayName("가장 최근 실행 상태가 COMPLETED(성공)인 경우 null을 반환한다")
    void validateAndGetLastFailedExecution_returnsNull_whenLastStatusIsCompleted() {
      // given: 최근 실행 결과가 COMPLETED 상태인 인스턴스 구성
      when(jobExplorer.findRunningJobExecutions("sportsJob")).thenReturn(Collections.emptySet());
      JobInstance instance = new JobInstance(1L, "sportsJob");
      when(jobExplorer.getJobInstances("sportsJob", 0, 1)).thenReturn(List.of(instance));

      JobExecution lastExecution = new JobExecution(instance, 10L, null);
      lastExecution.setStatus(BatchStatus.COMPLETED);
      when(jobExplorer.getJobExecutions(instance)).thenReturn(List.of(lastExecution));

      // when: 메서드를 호출할 때
      JobExecution result = batchAdminService.validateAndGetLastFailedExecution("sportsJob");

      // that: 복구 대상이 아니므로 null인지 검증한다
      assertThat(result).isNull();
    }

    @Test
    @DisplayName("가장 최근 실행 상태가 FAILED인 경우 해당 JobExecution을 정상 반환한다")
    void validateAndGetLastFailedExecution_returnsLastExecution_whenStatusIsFailed() {
      // given: 최근 실행 결과가 FAILED 상태인 인스턴스 구성
      when(jobExplorer.findRunningJobExecutions("sportsJob")).thenReturn(Collections.emptySet());
      JobInstance instance = new JobInstance(1L, "sportsJob");
      when(jobExplorer.getJobInstances("sportsJob", 0, 1)).thenReturn(List.of(instance));

      JobExecution failedExecution = new JobExecution(instance, 10L, null);
      failedExecution.setStatus(BatchStatus.FAILED);
      when(jobExplorer.getJobExecutions(instance)).thenReturn(List.of(failedExecution));

      // when: 메서드를 호출할 때
      JobExecution result = batchAdminService.validateAndGetLastFailedExecution("sportsJob");

      // that: FAILED 상태인 JobExecution이 정상 반환되는지 검증한다
      assertThat(result).isNotNull();
      assertThat(result.getId()).isEqualTo(10L);
    }
  }

  @Nested
  @DisplayName("2. restartJob(수동 재시작) 검증")
  class RestartJobTests {

    @Test
    @DisplayName("존재하지 않는 Job 이름으로 재시작 요청 시 JOB_NOT_FOUND 예외가 발생한다")
    void restartJob_fail_whenUnknownJobNameProvided() {
      // given: 알 수 없는 jobName 준비
      String unknownJob = "unknownJob";

      // when & that: restartJob 호출 시 JOB_NOT_FOUND 예외 검증
      assertThatThrownBy(() -> batchAdminService.restartJob(unknownJob))
          .isInstanceOf(BatchException.class)
          .extracting("errorCode")
          .isEqualTo(BatchErrorCode.JOB_NOT_FOUND);
    }

    @Test
    @DisplayName("재시작 가능한 FAILED 이력이 없는 경우 JOB_RESTART_UNAVAILABLE 예외가 발생한다")
    void restartJob_fail_whenNoFailedExecutionExists() {
      // given: 복구 대상(FAILED)이 없는 상황 설정
      when(jobExplorer.findRunningJobExecutions("sportsJob")).thenReturn(Collections.emptySet());
      when(jobExplorer.getJobInstances("sportsJob", 0, 1)).thenReturn(Collections.emptyList());

      // when & that: restartJob 호출 시 JOB_RESTART_UNAVAILABLE 예외 검증
      assertThatThrownBy(() -> batchAdminService.restartJob("sportsJob"))
          .isInstanceOf(BatchException.class)
          .extracting("errorCode")
          .isEqualTo(BatchErrorCode.JOB_RESTART_UNAVAILABLE);
    }

    @Test
    @DisplayName("재시작 가능한 FAILED 이력이 존재하는 경우 jobLauncher.run을 통해 재시작을 성공적으로 트리거한다")
    void restartJob_success_whenFailedExecutionExists() throws Exception {
      // given: 최근 실행이 FAILED 상태인 이력 설정
      when(jobExplorer.findRunningJobExecutions("sportsJob")).thenReturn(Collections.emptySet());
      JobInstance instance = new JobInstance(1L, "sportsJob");
      when(jobExplorer.getJobInstances("sportsJob", 0, 1)).thenReturn(List.of(instance));

      JobExecution failedExecution = new JobExecution(instance, 10L, null);
      failedExecution.setStatus(BatchStatus.FAILED);
      when(jobExplorer.getJobExecutions(instance)).thenReturn(List.of(failedExecution));

      // when: restartJob 호출
      batchAdminService.restartJob("sportsJob");

      // that: jobLauncher.run이 sportsJob 및 해당 파라미터와 함께 호출되었는지 검증한다
      verify(jobLauncher).run(eq(sportsJob), any(JobParameters.class));
    }
  }
}
