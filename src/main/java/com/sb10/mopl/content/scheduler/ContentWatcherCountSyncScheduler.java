package com.sb10.mopl.content.scheduler;

import com.sb10.mopl.content.service.ContentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/* 컨텐츠의 실시간 시청자 수(watcher_count) 정합성을 보정하는 정기 스케줄러입니다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContentWatcherCountSyncScheduler {

  private final ContentService contentService;

  /* 3분 간격으로 컨텐츠 반정규화 시청자 수 통계 정합성을 갱신합니다. */
  @Scheduled(fixedDelay = 3 * 60 * 1000)
  @SchedulerLock(name = "syncContentWatcherCountLock", lockAtMostFor = "9m", lockAtLeastFor = "30s")
  public void syncWatcherCount() {
    log.info("[CONTENT-WATCHER-SYNC] 컨텐츠 실시간 시청자 수 정합성 보정 시작");
    try {
      int updatedCount = contentService.syncWatcherCount();
      log.info("[CONTENT-WATCHER-SYNC] 컨텐츠 실시간 시청자 수 정합성 보정 완료 - 보정된 건수: {}건", updatedCount);
    } catch (Exception e) {
      log.error("[CONTENT-WATCHER-SYNC] 컨텐츠 실시간 시청자 수 정합성 보정 중 예외 발생", e);
    }
  }
}
