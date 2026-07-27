package com.sb10.mopl.watchingsession.scheduler;

import com.sb10.mopl.watchingsession.service.WatchingSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/* 매일 새벽 04시 30분에 24시간 이상 지난 만료/좀비 시청 세션을 삭제하는 정기 스케줄러입니다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class WatchingSessionCleanupScheduler {

  private final WatchingSessionService watchingSessionService;

  /* 매일 새벽 04시 30분에 만료된 좀비 시청 세션을 물리 삭제합니다. */
  @Scheduled(cron = "0 30 4 * * *", zone = "Asia/Seoul")
  @SchedulerLock(
      name = "cleanupExpiredWatchingSessionsLock",
      lockAtMostFor = "10m",
      lockAtLeastFor = "1m")
  public void cleanupExpiredSessions() {
    log.info("[WATCHING-SESSION-CLEANUP] 24시간 이상 만료된 좀비 시청 세션 정리 시작");
    try {
      int deletedCount = watchingSessionService.deleteExpiredSessions();
      log.info("[WATCHING-SESSION-CLEANUP] 만료된 좀비 시청 세션 정리 완료 - 삭제된 세션: {}건", deletedCount);
    } catch (Exception e) {
      log.error("[WATCHING-SESSION-CLEANUP] 만료된 좀비 시청 세션 정리 중 예외 발생", e);
    }
  }
}
