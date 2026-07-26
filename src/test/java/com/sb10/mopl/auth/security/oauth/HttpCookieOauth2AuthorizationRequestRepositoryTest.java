package com.sb10.mopl.auth.security.oauth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sb10.mopl.auth.security.jwt.JwtProperties;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

class HttpCookieOauth2AuthorizationRequestRepositoryTest {

  private final JwtProperties jwtProperties =
      new JwtProperties(
          "mopl-test-only-jwt-secret-key-at-least-32-bytes",
          Duration.ofHours(1),
          Duration.ofDays(14),
          new JwtProperties.RefreshTokenCookie("REFRESH_TOKEN", "/api/auth", true, true, "Lax"));

  private final HttpCookieOauth2AuthorizationRequestRepository repository =
      new HttpCookieOauth2AuthorizationRequestRepository(jwtProperties);

  @Test
  @DisplayName("저장한 인가 요청을 세션 없이 쿠키에서 그대로 읽어올 수 있다")
  void saveAndLoad_roundTrip() {
    // given
    OAuth2AuthorizationRequest authorizationRequest = googleAuthorizationRequest();
    MockHttpServletRequest saveRequest = new MockHttpServletRequest();
    MockHttpServletResponse saveResponse = new MockHttpServletResponse();

    // when
    repository.saveAuthorizationRequest(authorizationRequest, saveRequest, saveResponse);
    MockHttpServletRequest loadRequest = requestWithCookiesFrom(saveResponse);
    OAuth2AuthorizationRequest loaded = repository.loadAuthorizationRequest(loadRequest);

    // then
    assertEquals(authorizationRequest.getState(), loaded.getState());
    assertEquals(authorizationRequest.getClientId(), loaded.getClientId());
    assertEquals(authorizationRequest.getRedirectUri(), loaded.getRedirectUri());
    assertNull(saveRequest.getSession(false), "HttpSession이 생성되지 않아야 한다");
  }

  @Test
  @DisplayName("쿠키가 없으면 인가 요청 조회 시 null을 반환한다")
  void loadAuthorizationRequest_returnsNull_whenCookieMissing() {
    // given
    MockHttpServletRequest request = new MockHttpServletRequest();

    // when
    OAuth2AuthorizationRequest loaded = repository.loadAuthorizationRequest(request);

    // then
    assertNull(loaded);
  }

  @Test
  @DisplayName("인가 요청을 null로 저장하면 쿠키를 즉시 만료시킨다")
  void saveAuthorizationRequest_expiresCookie_whenRequestIsNull() {
    // given
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    // when
    repository.saveAuthorizationRequest(null, request, response);

    // then
    Cookie cookie =
        response.getCookie(
            HttpCookieOauth2AuthorizationRequestRepository.AUTHORIZATION_REQUEST_COOKIE_NAME);
    assertEquals(0, cookie.getMaxAge());
  }

  @Test
  @DisplayName("인가 요청을 제거하면 저장된 값을 반환하고 쿠키를 만료시킨다")
  void removeAuthorizationRequest_returnsStoredValueAndExpiresCookie() {
    // given
    OAuth2AuthorizationRequest authorizationRequest = googleAuthorizationRequest();
    MockHttpServletRequest saveRequest = new MockHttpServletRequest();
    MockHttpServletResponse saveResponse = new MockHttpServletResponse();
    repository.saveAuthorizationRequest(authorizationRequest, saveRequest, saveResponse);
    MockHttpServletRequest removeRequest = requestWithCookiesFrom(saveResponse);
    MockHttpServletResponse removeResponse = new MockHttpServletResponse();

    // when
    OAuth2AuthorizationRequest removed =
        repository.removeAuthorizationRequest(removeRequest, removeResponse);

    // then
    assertEquals(authorizationRequest.getState(), removed.getState());
    Cookie cookie =
        removeResponse.getCookie(
            HttpCookieOauth2AuthorizationRequestRepository.AUTHORIZATION_REQUEST_COOKIE_NAME);
    assertEquals(0, cookie.getMaxAge());
  }

  @Test
  @DisplayName("refresh token 쿠키의 sameSite가 Strict여도 OAUTH2_AUTH_REQUEST 쿠키는 항상 Lax로 발급된다")
  void saveAuthorizationRequest_alwaysUsesLaxSameSite_regardlessOfRefreshTokenCookieSetting() {
    // given
    JwtProperties strictJwtProperties =
        new JwtProperties(
            "mopl-test-only-jwt-secret-key-at-least-32-bytes",
            Duration.ofHours(1),
            Duration.ofDays(14),
            new JwtProperties.RefreshTokenCookie(
                "REFRESH_TOKEN", "/api/auth", true, true, "Strict"));
    HttpCookieOauth2AuthorizationRequestRepository strictRepository =
        new HttpCookieOauth2AuthorizationRequestRepository(strictJwtProperties);
    OAuth2AuthorizationRequest authorizationRequest = googleAuthorizationRequest();
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    // when
    strictRepository.saveAuthorizationRequest(authorizationRequest, request, response);

    // then
    String setCookieHeader = response.getHeader(HttpHeaders.SET_COOKIE);
    assertTrue(
        setCookieHeader.contains("SameSite=Lax"),
        "refresh token 쿠키 설정과 무관하게 OAUTH2_AUTH_REQUEST 쿠키는 SameSite=Lax여야 한다");
  }

  private OAuth2AuthorizationRequest googleAuthorizationRequest() {
    return OAuth2AuthorizationRequest.authorizationCode()
        .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
        .clientId("test-client-id")
        .redirectUri("https://mopl-sb10.click/login/oauth2/code/google")
        .scopes(Set.of("openid", "email"))
        .state("test-state")
        .build();
  }

  private MockHttpServletRequest requestWithCookiesFrom(MockHttpServletResponse response) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    for (Cookie cookie : response.getCookies()) {
      request.setCookies(cookie);
    }
    return request;
  }
}
