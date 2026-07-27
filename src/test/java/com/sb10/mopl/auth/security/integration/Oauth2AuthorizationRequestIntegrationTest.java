package com.sb10.mopl.auth.security.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sb10.mopl.auth.security.oauth.HttpCookieOauth2AuthorizationRequestRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.util.UriComponentsBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class Oauth2AuthorizationRequestIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  @DisplayName("구글 로그인 인가 요청은 HttpSession을 생성하지 않고 쿠키에 상태를 저장한다")
  void authorizationRequest_doesNotCreateHttpSession() throws Exception {
    // when
    MvcResult result =
        mockMvc
            .perform(get("/oauth2/authorization/google"))
            .andExpect(status().is3xxRedirection())
            .andReturn();

    // then
    assertNull(result.getRequest().getSession(false));
    String cookieName =
        HttpCookieOauth2AuthorizationRequestRepository.AUTHORIZATION_REQUEST_COOKIE_NAME;
    Cookie authRequestCookie = result.getResponse().getCookie(cookieName);
    assertNotNull(authRequestCookie);
  }

  @Test
  @DisplayName("OAuth2 로그인 콜백이 실패로 끝나도 OAUTH2_AUTH_REQUEST 쿠키는 즉시 만료된다")
  void authorizationRequest_isRemoved_whenCallbackFails() throws Exception {
    // given
    MvcResult authorizationResult =
        mockMvc
            .perform(get("/oauth2/authorization/google"))
            .andExpect(status().is3xxRedirection())
            .andReturn();

    String cookieName =
        HttpCookieOauth2AuthorizationRequestRepository.AUTHORIZATION_REQUEST_COOKIE_NAME;
    Cookie authRequestCookie = authorizationResult.getResponse().getCookie(cookieName);
    assertNotNull(authRequestCookie);
    String state = extractState(authorizationResult.getResponse().getRedirectedUrl());

    // when
    MvcResult callbackResult =
        mockMvc
            .perform(
                get("/login/oauth2/code/google")
                    .param("state", state)
                    .param("error", "access_denied")
                    .cookie(authRequestCookie))
            .andExpect(status().is3xxRedirection())
            .andReturn();

    // then
    assertTrue(callbackResult.getResponse().getRedirectedUrl().startsWith("/#/sign-in"));
    Cookie expiredCookie = callbackResult.getResponse().getCookie(cookieName);
    assertNotNull(expiredCookie);
    assertEquals(0, expiredCookie.getMaxAge());
  }

  private String extractState(String redirectUrl) {
    String state =
        UriComponentsBuilder.fromUriString(redirectUrl).build().getQueryParams().getFirst("state");
    assertNotNull(state, "state 파라미터가 존재해야 한다");
    return state;
  }
}
