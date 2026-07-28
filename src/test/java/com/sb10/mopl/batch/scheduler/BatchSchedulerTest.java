package com.sb10.mopl.batch.scheduler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.launch.JobLauncher;

@ExtendWith(MockitoExtension.class)
class BatchSchedulerTest {

  @Mock private JobLauncher jobLauncher;
  @Mock private Job tmdbJob;
  @Mock private Job sportsJob;

  @InjectMocks private BatchScheduler batchScheduler;

  @Nested
  @DisplayName("1. runSportsJob 정기 스케줄러 실행 및 예외 방어 검증")
  class RunSportsJobTests {

    @Test
    @DisplayName("runSportsJob 실행 시 jobLauncher를 통해 sportsJob 배치가 정상 실행된다")
    void runSportsJob_success_whenTriggered() throws Exception {
      // when: 스케줄러 메서드 실행
      batchScheduler.runSportsJob();

      // that: jobLauncher.run이 호출되는지 검증한다
      verify(jobLauncher).run(any(), any());
    }

    @Test
    @DisplayName("jobLauncher 실행 중 예외가 발생하더라도 스케줄러가 예외를 삼키고 외부로 던지지 않는다")
    void runSportsJob_doesNotThrow_whenJobLauncherThrowsException() throws Exception {
      // given: jobLauncher 호출 시 예외 발생 스터빙
      when(jobLauncher.run(any(), any())).thenThrow(new RuntimeException("배치 실행 실패"));

      // when & that: 예외 발생 없이 안전하게 처리되는지 검증한다
      assertDoesNotThrow(() -> batchScheduler.runSportsJob());
      verify(jobLauncher).run(any(), any());
    }
  }

  @Nested
  @DisplayName("2. runTmdbJob 정기 스케줄러 실행 및 예외 방어 검증")
  class RunTmdbJobTests {

    @Test
    @DisplayName("runTmdbJob 실행 시 jobLauncher를 통해 tmdbJob 배치가 정상 실행된다")
    void runTmdbJob_success_whenTriggered() throws Exception {
      // when: 스케줄러 메서드 실행
      batchScheduler.runTmdbJob();

      // that: jobLauncher.run이 호출되는지 검증한다
      verify(jobLauncher).run(any(), any());
    }

    @Test
    @DisplayName("jobLauncher 실행 중 예외가 발생하더라도 스케줄러가 예외를 삼키고 외부로 던지지 않는다")
    void runTmdbJob_doesNotThrow_whenJobLauncherThrowsException() throws Exception {
      // given: jobLauncher 호출 시 예외 발생 스터빙
      when(jobLauncher.run(any(), any())).thenThrow(new RuntimeException("배치 실행 실패"));

      // when & that: 예외 발생 없이 안전하게 처리되는지 검증한다
      assertDoesNotThrow(() -> batchScheduler.runTmdbJob());
      verify(jobLauncher).run(any(), any());
    }
  }
}
