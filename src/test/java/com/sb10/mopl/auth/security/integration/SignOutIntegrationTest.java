package com.sb10.mopl.auth.security.integration;

import static com.sb10.mopl.auth.security.integration.AuthIntegrationTestSupport.PROTECTED_API_PATH;
import static com.sb10.mopl.auth.security.integration.AuthIntegrationTestSupport.authenticatedGet;
import static com.sb10.mopl.auth.security.integration.AuthIntegrationTestSupport.expectAccessTokenUnauthorized;
import static com.sb10.mopl.auth.security.integration.AuthIntegrationTestSupport.expectRefreshTokenUnauthorized;
import static com.sb10.mopl.auth.security.integration.AuthIntegrationTestSupport.refreshTokenCookieName;
import static com.sb10.mopl.auth.security.integration.AuthIntegrationTestSupport.signIn;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sb10.mopl.auth.repository.JwtSessionRepository;
import com.sb10.mopl.auth.repository.RefreshTokenRepository;
import com.sb10.mopl.auth.security.integration.AuthIntegrationTestSupport.SignInTokens;
import com.sb10.mopl.auth.security.jwt.JwtProperties;
import com.sb10.mopl.auth.service.AuthSessionService;
import com.sb10.mopl.user.entity.User;
import com.sb10.mopl.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(AuthIntegrationTestSupport.ProtectedApiController.class)
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

  @Autowired private AuthSessionService authSessionService;

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
    SignInTokens tokens = signIn(mockMvc, objectMapper, jwtProperties, EMAIL, PASSWORD);

    mockMvc
        .perform(authenticatedGet(PROTECTED_API_PATH, tokens.accessToken()))
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
                    hasItem(
                        allOf(
                            containsString(refreshTokenCookieName(jwtProperties) + "=;"),
                            containsString("Max-Age=0")))));

    assertAll(
        () -> assertEquals(0L, jwtSessionRepository.count()),
        () -> assertEquals(0L, refreshTokenRepository.count()));

    expectAccessTokenUnauthorized(mockMvc, PROTECTED_API_PATH, tokens.accessToken());
    expectRefreshTokenUnauthorized(mockMvc, tokens.refreshToken());
  }

  @Test
  @DisplayName("이미 무효화된 인증 상태로 로그아웃해도 refresh token 쿠키를 정리한다")
  void signOut_clearsRefreshTokenCookie_whenAuthenticationStateAlreadyInvalidated()
      throws Exception {
    User user = saveUser();
    SignInTokens tokens = signIn(mockMvc, objectMapper, jwtProperties, EMAIL, PASSWORD);

    authSessionService.invalidateAllByUserId(user.getId());

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
                    hasItem(
                        allOf(
                            containsString(refreshTokenCookieName(jwtProperties) + "=;"),
                            containsString("Max-Age=0")))));

    assertAll(
        () -> assertEquals(0L, jwtSessionRepository.count()),
        () -> assertEquals(0L, refreshTokenRepository.count()));

    expectAccessTokenUnauthorized(mockMvc, PROTECTED_API_PATH, tokens.accessToken());
    expectRefreshTokenUnauthorized(mockMvc, tokens.refreshToken());
  }

  @Test
  @DisplayName("비로그인 사용자의 로그아웃 요청은 401을 반환한다")
  void signOut_returnsUnauthorized_whenUserIsAnonymous() throws Exception {
    mockMvc
        .perform(post("/api/auth/sign-out").with(csrf()))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTH01"));
  }

  private User saveUser() {
    User user = User.createUser("sign-out-user", EMAIL, passwordEncoder.encode(PASSWORD), null);
    return userRepository.saveAndFlush(user);
  }
}
