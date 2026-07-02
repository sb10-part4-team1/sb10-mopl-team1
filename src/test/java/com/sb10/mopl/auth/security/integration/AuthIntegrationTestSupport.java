package com.sb10.mopl.auth.security.integration;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sb10.mopl.auth.security.jwt.JwtProperties;
import jakarta.servlet.http.Cookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

final class AuthIntegrationTestSupport {

  private AuthIntegrationTestSupport() {}

  static SignInTokens signIn(
      MockMvc mockMvc,
      ObjectMapper objectMapper,
      JwtProperties jwtProperties,
      String email,
      String password)
      throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/auth/sign-in")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .param("username", email)
                    .param("password", password)
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
    String accessToken = response.get("accessToken").asText();
    Cookie refreshToken = result.getResponse().getCookie(refreshTokenCookieName(jwtProperties));

    assertAll(
        () -> assertTrue(!accessToken.isBlank()),
        () -> assertNotNull(refreshToken),
        () -> assertTrue(!refreshToken.getValue().isBlank()));

    return new SignInTokens(accessToken, refreshToken);
  }

  static MockHttpServletRequestBuilder authenticatedGet(String path, String accessToken) {
    return get(path).header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
  }

  static void expectAccessTokenUnauthorized(MockMvc mockMvc, String path, String accessToken)
      throws Exception {
    mockMvc
        .perform(authenticatedGet(path, accessToken))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTH01"));
  }

  static void expectRefreshTokenUnauthorized(MockMvc mockMvc, Cookie refreshToken)
      throws Exception {
    mockMvc
        .perform(post("/api/auth/refresh").cookie(refreshToken).with(csrf()))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTH01"));
  }

  static String refreshTokenCookieName(JwtProperties jwtProperties) {
    return jwtProperties.refreshTokenCookie().name();
  }

  record SignInTokens(String accessToken, Cookie refreshToken) {}
}
