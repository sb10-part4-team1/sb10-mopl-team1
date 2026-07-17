package com.sb10.mopl.auth.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sb10.mopl.auth.exception.AuthErrorCode;
import com.sb10.mopl.auth.service.AuthTokenService.IssuedToken;
import com.sb10.mopl.auth.service.AuthTokenService.ReissuedToken;
import com.sb10.mopl.auth.service.JwtSessionService.IssuedJwtSession;
import com.sb10.mopl.auth.service.RefreshTokenService.IssuedRefreshToken;
import com.sb10.mopl.auth.service.RefreshTokenService.RotatedRefreshToken;
import com.sb10.mopl.common.exception.MoplException;
import com.sb10.mopl.user.entity.User;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthTokenServiceTest {

  @Mock private RefreshTokenService refreshTokenService;

  @Mock private JwtSessionService jwtSessionService;

  @InjectMocks private AuthTokenService authTokenService;

  private UUID userId;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
  }

  @Test
  @DisplayName("발급 시 리프레시 토큰과 JWT 세션을 조합해 반환한다")
  void issue_success_combinesRefreshTokenAndJwtSession() {
    // given
    IssuedRefreshToken issuedRefreshToken = new IssuedRefreshToken("raw-token", Instant.now());
    UUID sessionId = UUID.randomUUID();
    IssuedJwtSession issuedJwtSession = new IssuedJwtSession(sessionId, Instant.now());
    when(refreshTokenService.issue(userId)).thenReturn(issuedRefreshToken);
    when(jwtSessionService.issue(userId)).thenReturn(issuedJwtSession);

    // when
    IssuedToken issuedToken = authTokenService.issue(userId);

    // then
    assertAll(
        () -> assertEquals(issuedRefreshToken, issuedToken.refreshToken()),
        () -> assertEquals(sessionId, issuedToken.sessionId()));
  }

  @Test
  @DisplayName("리프레시 토큰이 유효하고 세션이 연장되면 재발급에 성공한다")
  void reissue_success_whenRefreshTokenValidAndSessionExtends() {
    // given
    User user = user();
    Instant expiresAt = Instant.now().plusSeconds(60);
    IssuedRefreshToken issuedRefreshToken = new IssuedRefreshToken("new-raw-token", expiresAt);
    RotatedRefreshToken rotatedRefreshToken = new RotatedRefreshToken(issuedRefreshToken, user);
    UUID sessionId = UUID.randomUUID();
    when(refreshTokenService.rotate("old-raw-token")).thenReturn(Optional.of(rotatedRefreshToken));
    when(jwtSessionService.extendActiveSession(user.getId(), expiresAt))
        .thenReturn(Optional.of(sessionId));

    // when
    ReissuedToken reissuedToken = authTokenService.reissue("old-raw-token");

    // then
    assertAll(
        () -> assertEquals(issuedRefreshToken, reissuedToken.refreshToken()),
        () -> assertEquals(user, reissuedToken.user()),
        () -> assertEquals(sessionId, reissuedToken.sessionId()));
  }

  @Test
  @DisplayName("리프레시 토큰 회전이 실패하면 인증 실패 예외를 발생시키고 세션 연장은 시도하지 않는다")
  void reissue_throwsAuthenticationFailed_whenRefreshTokenRotateReturnsEmpty() {
    // given
    when(refreshTokenService.rotate("invalid-token")).thenReturn(Optional.empty());

    // when
    MoplException exception =
        assertThrows(MoplException.class, () -> authTokenService.reissue("invalid-token"));

    // then
    assertEquals(AuthErrorCode.AUTHENTICATION_FAILED, exception.getErrorCode());
    verify(refreshTokenService).rotate("invalid-token");
    verifyNoInteractions(jwtSessionService);
  }

  @Test
  @DisplayName("로그인 세션 연장이 실패하면 인증 실패 예외를 발생시킨다")
  void reissue_throwsAuthenticationFailed_whenExtendActiveSessionReturnsEmpty() {
    // given
    User user = user();
    Instant expiresAt = Instant.now().plusSeconds(60);
    IssuedRefreshToken issuedRefreshToken = new IssuedRefreshToken("new-raw-token", expiresAt);
    RotatedRefreshToken rotatedRefreshToken = new RotatedRefreshToken(issuedRefreshToken, user);
    when(refreshTokenService.rotate("old-raw-token")).thenReturn(Optional.of(rotatedRefreshToken));
    when(jwtSessionService.extendActiveSession(user.getId(), expiresAt))
        .thenReturn(Optional.empty());

    // when
    MoplException exception =
        assertThrows(MoplException.class, () -> authTokenService.reissue("old-raw-token"));

    // then
    assertEquals(AuthErrorCode.AUTHENTICATION_FAILED, exception.getErrorCode());
  }

  private User user() {
    User user = User.createUser("auth-token-user", "user@example.com", "encoded-password", null);
    ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
    return user;
  }
}
