package com.sb10.mopl.auth.scheduler;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sb10.mopl.auth.service.JwtSessionService;
import com.sb10.mopl.auth.service.RefreshTokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthCleanupSchedulerTest {

  @Mock private RefreshTokenService refreshTokenService;

  @Mock private JwtSessionService jwtSessionService;

  @InjectMocks private AuthCleanupScheduler authCleanupScheduler;

  @Test
  @DisplayName("만료된 리프레시 토큰과 JWT 세션을 각각 정리한다")
  void cleanupExpiredAuthTokens_deletesExpiredTokensAndSessions() {
    // given
    when(refreshTokenService.deleteExpiredTokens()).thenReturn(3);
    when(jwtSessionService.deleteExpiredSessions()).thenReturn(5);

    // when
    authCleanupScheduler.cleanupExpiredAuthTokens();

    // then
    verify(refreshTokenService).deleteExpiredTokens();
    verify(jwtSessionService).deleteExpiredSessions();
  }

  @Test
  @DisplayName("리프레시 토큰 정리 중 예외가 발생해도 JWT 세션 정리는 계속 수행한다")
  void cleanupExpiredAuthTokens_continuesJwtSessionCleanup_whenRefreshTokenCleanupFails() {
    // given
    when(refreshTokenService.deleteExpiredTokens()).thenThrow(new RuntimeException("db error"));
    when(jwtSessionService.deleteExpiredSessions()).thenReturn(2);

    // when
    authCleanupScheduler.cleanupExpiredAuthTokens();

    // then
    verify(jwtSessionService).deleteExpiredSessions();
  }

  @Test
  @DisplayName("JWT 세션 정리 중 예외가 발생해도 리프레시 토큰 정리는 이미 수행된 상태를 유지한다")
  void cleanupExpiredAuthTokens_keepsRefreshTokenCleanupResult_whenJwtSessionCleanupFails() {
    // given
    when(refreshTokenService.deleteExpiredTokens()).thenReturn(4);
    when(jwtSessionService.deleteExpiredSessions()).thenThrow(new RuntimeException("db error"));

    // when
    authCleanupScheduler.cleanupExpiredAuthTokens();

    // then
    verify(refreshTokenService).deleteExpiredTokens();
  }
}
