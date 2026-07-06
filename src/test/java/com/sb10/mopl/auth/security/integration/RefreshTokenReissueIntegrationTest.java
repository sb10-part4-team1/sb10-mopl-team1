package com.sb10.mopl.auth.security.integration;

import static com.sb10.mopl.auth.security.RefreshTokenCookieAssertions.expectRefreshTokenCookie;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sb10.mopl.auth.entity.RefreshToken;
import com.sb10.mopl.auth.repository.RefreshTokenRepository;
import com.sb10.mopl.auth.security.jwt.JwtProperties;
import com.sb10.mopl.auth.security.jwt.JwtProvider;
import com.sb10.mopl.user.entity.User;
import com.sb10.mopl.user.entity.UserRole;
import com.sb10.mopl.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RefreshTokenReissueIntegrationTest {

  private static final String EMAIL = "refresh-user@example.com";
  private static final String PASSWORD = "password123";

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private UserRepository userRepository;

  @Autowired private RefreshTokenRepository refreshTokenRepository;

  @Autowired private PasswordEncoder passwordEncoder;

  @Autowired private JwtProvider jwtProvider;

  @Autowired private JwtProperties jwtProperties;

  @Autowired private Clock clock;

  @BeforeEach
  void setUp() {
    refreshTokenRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  @DisplayName("정상 리프레시 토큰으로 access token과 refresh token을 재발급한다")
  void refresh_success_whenRefreshTokenIsValid() throws Exception {
    User user = saveUser();
    Cookie originalRefreshToken = signInAndGetRefreshToken();

    MvcResult result =
        expectRefreshTokenCookie(
                mockMvc
                    .perform(post("/api/auth/refresh").cookie(originalRefreshToken).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                    .andExpect(header().string(HttpHeaders.PRAGMA, "no-cache")),
                jwtProperties)
            .andExpect(jsonPath("$.userDto.id").value(user.getId().toString()))
            .andExpect(jsonPath("$.userDto.email").value(EMAIL))
            .andExpect(jsonPath("$.userDto.role").value(UserRole.USER.name()))
            .andExpect(jsonPath("$.accessToken").isString())
            .andReturn();

    Cookie rotatedRefreshToken = result.getResponse().getCookie(refreshTokenCookieName());
    JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
    Claims accessTokenClaims = jwtProvider.parseClaims(response.get("accessToken").asText());

    assertAll(
        () -> assertNotNull(rotatedRefreshToken),
        () -> assertNotEquals(originalRefreshToken.getValue(), rotatedRefreshToken.getValue()),
        () -> assertEquals(1, refreshTokenRepository.count()),
        () -> assertEquals(user.getId().toString(), accessTokenClaims.getSubject()),
        () -> assertEquals(EMAIL, accessTokenClaims.get("email")),
        () -> assertEquals(UserRole.USER.name(), accessTokenClaims.get("role")),
        () ->
            assertEquals(
                JwtProvider.ACCESS_TOKEN_TYPE,
                accessTokenClaims.get(JwtProvider.TOKEN_TYPE_CLAIM)));

    expectRefreshTokenCookie(
            mockMvc
                .perform(post("/api/auth/refresh").cookie(rotatedRefreshToken).with(csrf()))
                .andExpect(status().isOk()),
            jwtProperties)
        .andExpect(jsonPath("$.accessToken").isString());
  }

  @Test
  @DisplayName("토큰 재발급 후 기존 리프레시 토큰은 재사용할 수 없다")
  void refresh_fail_whenRefreshTokenIsReusedAfterRotation() throws Exception {
    saveUser();
    Cookie originalRefreshToken = signInAndGetRefreshToken();

    mockMvc
        .perform(post("/api/auth/refresh").cookie(originalRefreshToken).with(csrf()))
        .andExpect(status().isOk());

    mockMvc
        .perform(post("/api/auth/refresh").cookie(originalRefreshToken).with(csrf()))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTH01"));
  }

  @Test
  @DisplayName("리프레시 토큰 쿠키가 없으면 401을 반환한다")
  void refresh_fail_whenRefreshTokenCookieIsMissing() throws Exception {
    mockMvc
        .perform(post("/api/auth/refresh").with(csrf()))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTH01"));
  }

  @Test
  @DisplayName("만료된 리프레시 토큰이면 401을 반환한다")
  void refresh_fail_whenRefreshTokenIsExpired() throws Exception {
    User user = saveUser();
    String rawExpiredToken = "expired-refresh-token";
    refreshTokenRepository.saveAndFlush(
        RefreshToken.create(user, hash(rawExpiredToken), clock.instant().minusSeconds(1)));

    mockMvc
        .perform(
            post("/api/auth/refresh")
                .cookie(new Cookie(refreshTokenCookieName(), rawExpiredToken))
                .with(csrf()))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTH01"));
  }

  @Test
  @DisplayName("저장소에 없는 리프레시 토큰이면 401을 반환한다")
  void refresh_fail_whenRefreshTokenIsUnknown() throws Exception {
    saveUser();

    mockMvc
        .perform(
            post("/api/auth/refresh")
                .cookie(new Cookie(refreshTokenCookieName(), "unknown-refresh-token"))
                .with(csrf()))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTH01"));
  }

  private Cookie signInAndGetRefreshToken() throws Exception {
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

    Cookie refreshToken = result.getResponse().getCookie(refreshTokenCookieName());
    assertNotNull(refreshToken);
    assertTrue(!refreshToken.getValue().isBlank());
    return refreshToken;
  }

  private User saveUser() {
    User user = User.createUser("refresh-user", EMAIL, passwordEncoder.encode(PASSWORD), null);
    return userRepository.saveAndFlush(user);
  }

  private String refreshTokenCookieName() {
    return jwtProperties.refreshTokenCookie().name();
  }

  private String hash(String rawToken) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 algorithm is not available.", exception);
    }
  }
}
