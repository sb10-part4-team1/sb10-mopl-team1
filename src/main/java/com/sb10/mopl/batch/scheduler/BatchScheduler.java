package com.sb10.mopl.batch.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/* 매일 정해진 시간에 스포츠 및 TMDB 인기 콘텐츠 수집 배치를 각각 기동하는 통합 스케줄러 클래스입니다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class BatchScheduler {

  private final JobLauncher jobLauncher;
  private final Job tmdbJob;
  private final Job sportsJob;

  /* 매일 새벽 1시에 스포츠 경기 수집 배치를 단독 실행합니다. */
  @Scheduled(cron = "0 0 1 * * *", zone = "Asia/Seoul")
  @SchedulerLock(name = "runSportsJobLock", lockAtMostFor = "10m", lockAtLeastFor = "1m")
  public void runSportsJob() {
    long timestamp = System.currentTimeMillis();
    log.info("[SCHEDULE-SPORTS] 정기 스포츠 수집 배치 시작 - timestamp: {}", timestamp);

    try {
      jobLauncher.run(
          sportsJob, new JobParametersBuilder().addLong("time", timestamp).toJobParameters());
      log.info("[SCHEDULE-SPORTS] 정기 스포츠 수집 배치 완료");
    } catch (Exception e) {
      log.error("[SCHEDULE-SPORTS] 정기 스포츠 수집 배치 실행 중 예외 발생", e);
    }
  }

  /* 매일 새벽 1시에 TMDB 인기 콘텐츠 수집 배치를 단독 실행합니다. */
  @Scheduled(cron = "0 0 1 * * *", zone = "Asia/Seoul")
  @SchedulerLock(name = "runTmdbJobLock", lockAtMostFor = "10m", lockAtLeastFor = "1m")
  public void runTmdbJob() {
    long timestamp = System.currentTimeMillis();
    log.info("[SCHEDULE-TMDB] 정기 TMDB 수집 배치 시작 - timestamp: {}", timestamp);

    try {
      jobLauncher.run(
          tmdbJob, new JobParametersBuilder().addLong("time", timestamp).toJobParameters());
      log.info("[SCHEDULE-TMDB] 정기 TMDB 수집 배치 완료");
    } catch (Exception e) {
      log.error("[SCHEDULE-TMDB] 정기 TMDB 수집 배치 실행 중 예외 발생", e);
    }
  }
}
