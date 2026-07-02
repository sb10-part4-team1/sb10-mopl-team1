package com.sb10.mopl.auth.security.integration;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
@Import(SignOutIntegrationTest.ProtectedApiController.class)
class SignOutIntegrationTest {

  private static final String EMAIL = "sign-out-user@example.com";
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
  @DisplayName("로그아웃하면 refresh token 쿠키와 서버 인증 세션이 정리된다")
  void signOut_invalidatesAuthenticationState_whenUserIsAuthenticated() throws Exception {
    saveUser();
    SignInTokens tokens = signIn();

    mockMvc
        .perform(authenticatedProtectedApi(tokens.accessToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("authenticated"));

    mockMvc
        .perform(
            post("/api/auth/sign-out")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.accessToken())
                .cookie(tokens.refreshToken())
                .with(csrf()))
        .andExpect(status().isNoContent())
        .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
        .andExpect(header().string(HttpHeaders.PRAGMA, "no-cache"))
        .andExpect(
            header()
                .stringValues(
                    HttpHeaders.SET_COOKIE,
                    hasItem(containsString(refreshTokenCookieName() + "=;"))))
        .andExpect(
            header()
                .stringValues(HttpHeaders.SET_COOKIE, hasItem(containsString("Max-Age=0"))));

    assertAll(
        () -> assertEquals(0L, jwtSessionRepository.count()),
        () -> assertEquals(0L, refreshTokenRepository.count()));

    mockMvc
        .perform(authenticatedProtectedApi(tokens.accessToken()))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTH01"));

    mockMvc
        .perform(post("/api/auth/refresh").cookie(tokens.refreshToken()).with(csrf()))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTH01"));
  }

  @Test
  @DisplayName("비로그인 사용자의 로그아웃 요청은 401을 반환한다")
  void signOut_returnsUnauthorized_whenUserIsAnonymous() throws Exception {
    mockMvc
        .perform(post("/api/auth/sign-out").with(csrf()))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTH01"));
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
    return get("/api/test/sign-out/protected")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
  }

  private User saveUser() {
    User user = User.createUser("sign-out-user", EMAIL, passwordEncoder.encode(PASSWORD), null);
    return userRepository.saveAndFlush(user);
  }

  private String refreshTokenCookieName() {
    return jwtProperties.refreshTokenCookie().name();
  }

  private record SignInTokens(String accessToken, Cookie refreshToken) {}

  @RestController
  static class ProtectedApiController {

    @GetMapping("/api/test/sign-out/protected")
    MessageResponse protectedApi() {
      return new MessageResponse("authenticated");
    }
  }

  record MessageResponse(String message) {}
}
