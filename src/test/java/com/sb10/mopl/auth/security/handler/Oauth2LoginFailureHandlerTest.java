package com.sb10.mopl.auth.security.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.sb10.mopl.auth.exception.AuthErrorCode;
import com.sb10.mopl.common.exception.MoplException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.test.util.ReflectionTestUtils;

class Oauth2LoginFailureHandlerTest {

  @Test
  @DisplayName("알 수 없는 소셜 로그인 실패는 기본 메시지로 로그인 화면에 리다이렉트한다")
  void login_failure_redirectsToSignInWithDefaultErrorMessage() throws Exception {
    Oauth2LoginFailureHandler failureHandler = new Oauth2LoginFailureHandler();
    MockHttpServletResponse response = new MockHttpServletResponse();
    ReflectionTestUtils.setField(failureHandler, "failureRedirectUri", "/#/sign-in");

    failureHandler.onAuthenticationFailure(
        new MockHttpServletRequest(),
        response,
        new AuthenticationServiceException("OAuth2 login failed."));

    String redirectUrl = response.getRedirectedUrl();

    assertEquals(302, response.getStatus());
    Assertions.assertNotNull(redirectUrl);
    assertEquals(
        "/#/sign-in?error=oauth_failed&error_message="
            + URLEncoder.encode("소셜 로그인에 실패했습니다.", StandardCharsets.UTF_8),
        redirectUrl);
  }

  @Test
  @DisplayName("실패 리다이렉트 URI에 쿼리가 있으면 오류 쿼리 파라미터를 이어 붙인다")
  void login_failure_appendsErrorQueryParametersToExistingQuery() throws Exception {
    Oauth2LoginFailureHandler failureHandler = new Oauth2LoginFailureHandler();
    MockHttpServletResponse response = new MockHttpServletResponse();
    ReflectionTestUtils.setField(failureHandler, "failureRedirectUri", "/#/sign-in?from=oauth2");

    failureHandler.onAuthenticationFailure(
        new MockHttpServletRequest(),
        response,
        new AuthenticationServiceException("OAuth2 login failed."));

    Assertions.assertNotNull(response.getRedirectedUrl());
    Assertions.assertTrue(
        response
            .getRedirectedUrl()
            .startsWith("/#/sign-in?from=oauth2&error=oauth_failed&error_message="));
  }

  @Test
  @DisplayName("도메인 예외의 사용자 메시지를 소셜 로그인 실패 리다이렉트에 포함한다")
  void login_failure_redirectsWithMoplExceptionMessage() throws Exception {
    Oauth2LoginFailureHandler failureHandler = new Oauth2LoginFailureHandler();
    MockHttpServletResponse response = new MockHttpServletResponse();
    ReflectionTestUtils.setField(failureHandler, "failureRedirectUri", "/#/sign-in");

    failureHandler.onAuthenticationFailure(
        new MockHttpServletRequest(),
        response,
        new AuthenticationServiceException(
            "OAuth2 login failed.",
            new MoplException(
                AuthErrorCode.AUTHENTICATION_FAILED, Map.of("message", "잠긴 계정은 로그인할 수 없습니다."))));

    assertEquals("잠긴 계정은 로그인할 수 없습니다.", errorMessage(response));
  }

  @Test
  @DisplayName("OAuth 접근 거부는 취소 또는 권한 거부 메시지로 로그인 화면에 리다이렉트한다")
  void login_failure_redirectsWithAccessDeniedMessage() throws Exception {
    Oauth2LoginFailureHandler failureHandler = new Oauth2LoginFailureHandler();
    MockHttpServletResponse response = new MockHttpServletResponse();
    ReflectionTestUtils.setField(failureHandler, "failureRedirectUri", "/#/sign-in");

    failureHandler.onAuthenticationFailure(
        new MockHttpServletRequest(),
        response,
        new OAuth2AuthenticationException(
            new OAuth2Error("access_denied", "provider error message", null)));

    assertEquals("소셜 로그인 요청이 취소되었거나 권한이 거부되었습니다.", errorMessage(response));
  }

  @Test
  @DisplayName("접근 거부 이외의 OAuth 오류는 기본 메시지로 로그인 화면에 리다이렉트한다")
  void login_failure_redirectsWithDefaultMessageForOtherOauth2Error() throws Exception {
    Oauth2LoginFailureHandler failureHandler = new Oauth2LoginFailureHandler();
    MockHttpServletResponse response = new MockHttpServletResponse();
    ReflectionTestUtils.setField(failureHandler, "failureRedirectUri", "/#/sign-in");

    failureHandler.onAuthenticationFailure(
        new MockHttpServletRequest(),
        response,
        new OAuth2AuthenticationException(
            new OAuth2Error("invalid_token", "provider error message", null)));

    assertEquals("소셜 로그인에 실패했습니다.", errorMessage(response));
  }

  private String errorMessage(MockHttpServletResponse response) {
    String redirectUrl = response.getRedirectedUrl();
    Assertions.assertNotNull(redirectUrl);
    String encodedMessage = redirectUrl.substring(redirectUrl.indexOf("error_message=") + 14);
    return URLDecoder.decode(encodedMessage, StandardCharsets.UTF_8);
  }
}
