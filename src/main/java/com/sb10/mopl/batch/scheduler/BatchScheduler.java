package com.sb10.mopl.batch.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 매일 정해진 시간에 영화/TV 및 스포츠 콘텐츠 수집 배치를 자동으로 실행하는 스케줄러 클래스입니다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class BatchScheduler {

  private final JobLauncher jobLauncher;
  private final Job tmdbJob;
  private final Job sportsJob;

  /** 매일 자정 + 2(2시 0분 0초)에 TMDB 및 스포츠 경기 수집 배치를 순차적으로 실행합니다. */
  @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Seoul")
  public void runJobs() {
    long timestamp = System.currentTimeMillis();
    log.info("정기 배치 스케줄러 작동 시작 - timestamp: {}", timestamp);

    // 1. TMDB 수집 배치 실행
    try {
      log.info("정기 TMDB 수집 배치 잡 실행 시작");
      jobLauncher.run(
          tmdbJob, new JobParametersBuilder().addLong("time", timestamp).toJobParameters());
      log.info("정기 TMDB 수집 배치 잡 실행 완료");
    } catch (Exception e) {
      log.error("정기 TMDB 수집 배치 잡 실행 중 예외 발생", e);
    }

    // 2. Sports 수집 배치 실행
    try {
      log.info("정기 Sports 수집 배치 잡 실행 시작");
      jobLauncher.run(
          sportsJob, new JobParametersBuilder().addLong("time", timestamp).toJobParameters());
      log.info("정기 Sports 수집 배치 잡 실행 완료");
    } catch (Exception e) {
      log.error("정기 Sports 수집 배치 잡 실행 중 예외 발생", e);
    }

    log.info("정기 배치 스케줄러 작동 종료");
  }
}
