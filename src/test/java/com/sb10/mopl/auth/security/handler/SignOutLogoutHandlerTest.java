package com.sb10.mopl.auth.security.handler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sb10.mopl.auth.security.cookie.RefreshTokenCookieResolver;
import com.sb10.mopl.auth.security.cookie.RefreshTokenCookieWriter;
import com.sb10.mopl.auth.security.user.AuthenticatedUser;
import com.sb10.mopl.auth.service.AuthSessionService;
import com.sb10.mopl.auth.service.RefreshTokenService;
import com.sb10.mopl.user.entity.UserRole;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class SignOutLogoutHandlerTest {

  @Mock private AuthSessionService authSessionService;

  @Mock private RefreshTokenService refreshTokenService;

  @Mock private RefreshTokenCookieResolver refreshTokenCookieResolver;

  @Mock private RefreshTokenCookieWriter refreshTokenCookieWriter;

  private SignOutLogoutHandler handler;
  private MockHttpServletRequest request;
  private MockHttpServletResponse response;

  @BeforeEach
  void setUp() {
    handler =
        new SignOutLogoutHandler(
            authSessionService,
            refreshTokenService,
            refreshTokenCookieResolver,
            refreshTokenCookieWriter);
    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
  }

  @Test
  @DisplayName("정상 로그아웃 시 세션 무효화, 토큰 회수, 쿠키 만료를 모두 수행한다")
  void logout_success_invalidatesSessionRevokesTokenAndExpiresCookie() {
    // given
    UUID userId = UUID.randomUUID();
    Authentication authentication = authenticationOf(userId);
    when(refreshTokenCookieResolver.resolve(request)).thenReturn(Optional.of("raw-token"));

    // when
    handler.logout(request, response, authentication);

    // then
    verify(authSessionService).invalidateAllByUserId(userId);
    verify(refreshTokenService).revoke("raw-token");
    verify(refreshTokenCookieWriter).expireRefreshTokenCookie(response);
  }

  @Test
  @DisplayName("세션 무효화 중 예외가 발생해도 토큰 회수와 쿠키 만료는 계속 진행한다")
  void logout_continues_whenSessionInvalidationThrows() {
    // given
    UUID userId = UUID.randomUUID();
    Authentication authentication = authenticationOf(userId);
    when(refreshTokenCookieResolver.resolve(request)).thenReturn(Optional.of("raw-token"));
    doThrow(new RuntimeException("session error"))
        .when(authSessionService)
        .invalidateAllByUserId(any());

    // when & then
    assertDoesNotThrow(() -> handler.logout(request, response, authentication));
    verify(refreshTokenService).revoke("raw-token");
    verify(refreshTokenCookieWriter).expireRefreshTokenCookie(response);
  }

  @Test
  @DisplayName("토큰 회수 중 예외가 발생해도 쿠키 만료는 finally로 항상 수행한다")
  void logout_continues_whenTokenRevocationThrows() {
    // given
    UUID userId = UUID.randomUUID();
    Authentication authentication = authenticationOf(userId);
    when(refreshTokenCookieResolver.resolve(request)).thenReturn(Optional.of("raw-token"));
    doThrow(new RuntimeException("revoke error")).when(refreshTokenService).revoke("raw-token");

    // when & then
    assertDoesNotThrow(() -> handler.logout(request, response, authentication));
    verify(refreshTokenCookieWriter).expireRefreshTokenCookie(response);
  }

  @Test
  @DisplayName("principal이 AuthenticatedUser가 아니면 세션 무효화는 건너뛰고 쿠키는 만료시킨다")
  void logout_skipsSessionInvalidation_whenPrincipalIsNotAuthenticatedUser() {
    // given
    Authentication authentication = mock(Authentication.class);
    when(authentication.getPrincipal()).thenReturn("anonymousUser");
    when(refreshTokenCookieResolver.resolve(request)).thenReturn(Optional.empty());

    // when
    handler.logout(request, response, authentication);

    // then
    verify(authSessionService, never()).invalidateAllByUserId(any());
    verify(refreshTokenCookieWriter).expireRefreshTokenCookie(response);
  }

  @Test
  @DisplayName("리프레시 토큰 쿠키가 없으면 토큰 회수는 건너뛰고 쿠키는 만료시킨다")
  void logout_skipsTokenRevocation_whenNoCookiePresent() {
    // given
    UUID userId = UUID.randomUUID();
    Authentication authentication = authenticationOf(userId);
    when(refreshTokenCookieResolver.resolve(request)).thenReturn(Optional.empty());

    // when
    handler.logout(request, response, authentication);

    // then
    verify(refreshTokenService, never()).revoke(any());
    verify(refreshTokenCookieWriter).expireRefreshTokenCookie(response);
  }

  private Authentication authenticationOf(UUID userId) {
    Authentication authentication = mock(Authentication.class);
    when(authentication.getPrincipal())
        .thenReturn(new AuthenticatedUser(userId, "user@example.com", UserRole.USER));
    return authentication;
  }
}
