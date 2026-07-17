package com.sb10.mopl.auth.scheduler;

import com.sb10.mopl.auth.service.JwtSessionService;
import com.sb10.mopl.auth.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthCleanupScheduler {

  private final RefreshTokenService refreshTokenService;
  private final JwtSessionService jwtSessionService;

  @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
  @SchedulerLock(
      name = "cleanupExpiredAuthTokensLock",
      lockAtMostFor = "10m",
      lockAtLeastFor = "1m")
  public void cleanupExpiredAuthTokens() {
    try {
      int deletedTokens = refreshTokenService.deleteExpiredTokens();
      log.info("[AUTH-CLEANUP] 만료 리프레시 토큰 정리 완료 - {}건", deletedTokens);
    } catch (Exception e) {
      log.error("[AUTH-CLEANUP] 만료 리프레시 토큰 정리 중 예외 발생", e);
    }

    try {
      int deletedSessions = jwtSessionService.deleteExpiredSessions();
      log.info("[AUTH-CLEANUP] 만료 JWT 세션 정리 완료 - {}건", deletedSessions);
    } catch (Exception e) {
      log.error("[AUTH-CLEANUP] 만료 JWT 세션 정리 중 예외 발생", e);
    }
  }
}
