package com.sb10.mopl.content.scheduler;

import com.sb10.mopl.content.service.ContentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/* 매일 새벽 05시 00분에 컨텐츠의 리뷰 수 및 평균 평점 정합성을 보정하는 정기 스케줄러입니다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContentReviewSyncScheduler {

  private final ContentService contentService;

  /* 매일 새벽 05시 00분에 컨텐츠 반정규화 리뷰 통계 정합성을 갱신합니다. */
  @Scheduled(cron = "0 0 5 * * *", zone = "Asia/Seoul")
  @SchedulerLock(
      name = "syncContentReviewStatisticsLock",
      lockAtMostFor = "10m",
      lockAtLeastFor = "1m")
  public void syncReviewStatistics() {
    log.info("[CONTENT-REVIEW-SYNC] 컨텐츠 리뷰 수 및 평균 평점 정합성 보정 시작");
    try {
      int updatedCount = contentService.syncReviewStatistics();
      log.info("[CONTENT-REVIEW-SYNC] 컨텐츠 리뷰 수 및 평균 평점 정합성 보정 완료 - 보정된 건수: {}건", updatedCount);
    } catch (Exception e) {
      log.error("[CONTENT-REVIEW-SYNC] 컨텐츠 리뷰 수 및 평균 평점 정합성 보정 중 예외 발생", e);
    }
  }
}
