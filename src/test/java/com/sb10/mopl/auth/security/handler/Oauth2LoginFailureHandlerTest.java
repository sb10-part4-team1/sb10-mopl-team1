package com.sb10.mopl.auth.security.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.test.util.ReflectionTestUtils;

class Oauth2LoginFailureHandlerTest {

  @Test
  @DisplayName("소셜 로그인에 실패하면 오류 정보를 포함해 로그인 화면으로 리다이렉트한다")
  void login_failure_redirectsToSignInWithErrorQueryParameters() throws Exception {
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
    assertTrue(redirectUrl.startsWith("/#/sign-in?error=oauth_failed&error_message="));
    assertTrue(redirectUrl.contains(URLEncoder.encode("소셜 로그인", StandardCharsets.UTF_8)));
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

    assertTrue(
        response
            .getRedirectedUrl()
            .startsWith("/#/sign-in?from=oauth2&error=oauth_failed&error_message="));
  }
}
