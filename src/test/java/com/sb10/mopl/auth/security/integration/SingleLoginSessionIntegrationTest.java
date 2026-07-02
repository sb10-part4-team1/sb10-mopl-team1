package com.sb10.mopl.auth.security.integration;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sb10.mopl.auth.repository.JwtSessionRepository;
import com.sb10.mopl.auth.repository.RefreshTokenRepository;
import com.sb10.mopl.auth.security.jwt.JwtProperties;
import com.sb10.mopl.user.entity.User;
import com.sb10.mopl.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(SingleLoginSessionIntegrationTest.ProtectedApiController.class)
class SingleLoginSessionIntegrationTest {

  private static final String EMAIL = "single-login-user@example.com";
  private static final String PASSWORD = "password123";

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private UserRepository userRepository;

  @Autowired private RefreshTokenRepository refreshTokenRepository;

  @Autowired private JwtSessionRepository jwtSessionRepository;

  @Autowired private PasswordEncoder passwordEncoder;

  @Autowired private JwtProperties jwtProperties;

  @BeforeEach
  void setUp() {
    refreshTokenRepository.deleteAll();
    jwtSessionRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  @DisplayName("동일 사용자가 다시 로그인하면 기존 access token과 refresh token은 무효화된다")
  void signIn_invalidatesPreviousSessionTokens_whenSameUserSignsInAgain() throws Exception {
    saveUser();
    SignInTokens firstTokens = signIn();

    mockMvc
        .perform(authenticatedProtectedApi(firstTokens.accessToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("authenticated"));

    SignInTokens secondTokens = signIn();

    assertAll(
        () -> assertNotEquals(firstTokens.accessToken(), secondTokens.accessToken()),
        () ->
            assertNotEquals(
                firstTokens.refreshToken().getValue(), secondTokens.refreshToken().getValue()),
        () -> assertEquals(1L, jwtSessionRepository.count()),
        () -> assertEquals(1L, refreshTokenRepository.count()));

    mockMvc
        .perform(authenticatedProtectedApi(firstTokens.accessToken()))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTH01"));

    mockMvc
        .perform(post("/api/auth/refresh").cookie(firstTokens.refreshToken()).with(csrf()))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTH01"));

    mockMvc
        .perform(authenticatedProtectedApi(secondTokens.accessToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("authenticated"));
  }

  private SignInTokens signIn() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/auth/sign-in")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .param("username", EMAIL)
                    .param("password", PASSWORD)
                    .with(csrf()))
            .andExpect(status().isOk())
            .andReturn();

    JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
    String accessToken = response.get("accessToken").asText();
    Cookie refreshToken = result.getResponse().getCookie(refreshTokenCookieName());

    assertAll(
        () -> assertTrue(!accessToken.isBlank()),
        () -> assertNotNull(refreshToken),
        () -> assertTrue(!refreshToken.getValue().isBlank()));

    return new SignInTokens(accessToken, refreshToken);
  }

  private MockHttpServletRequestBuilder authenticatedProtectedApi(String accessToken) {
    return get("/api/test/auth-session/protected")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
  }

  private User saveUser() {
    User user =
        User.createUser("single-login-user", EMAIL, passwordEncoder.encode(PASSWORD), null);
    return userRepository.saveAndFlush(user);
  }

  private String refreshTokenCookieName() {
    return jwtProperties.refreshTokenCookie().name();
  }

  private record SignInTokens(String accessToken, Cookie refreshToken) {}

  @RestController
  static class ProtectedApiController {

    @GetMapping("/api/test/auth-session/protected")
    MessageResponse protectedApi() {
      return new MessageResponse("authenticated");
    }
  }

  record MessageResponse(String message) {}
}
