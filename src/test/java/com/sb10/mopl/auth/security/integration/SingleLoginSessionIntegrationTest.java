package com.sb10.mopl.auth.security.integration;

import static com.sb10.mopl.auth.security.integration.AuthIntegrationTestSupport.PROTECTED_API_PATH;
import static com.sb10.mopl.auth.security.integration.AuthIntegrationTestSupport.authenticatedGet;
import static com.sb10.mopl.auth.security.integration.AuthIntegrationTestSupport.expectAccessTokenUnauthorized;
import static com.sb10.mopl.auth.security.integration.AuthIntegrationTestSupport.expectRefreshTokenUnauthorized;
import static com.sb10.mopl.auth.security.integration.AuthIntegrationTestSupport.signIn;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sb10.mopl.auth.repository.JwtSessionRepository;
import com.sb10.mopl.auth.repository.RefreshTokenRepository;
import com.sb10.mopl.auth.security.integration.AuthIntegrationTestSupport.SignInTokens;
import com.sb10.mopl.auth.security.jwt.JwtProperties;
import com.sb10.mopl.user.entity.User;
import com.sb10.mopl.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(AuthIntegrationTestSupport.ProtectedApiController.class)
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
    SignInTokens firstTokens = signIn(mockMvc, objectMapper, jwtProperties, EMAIL, PASSWORD);

    mockMvc
        .perform(authenticatedGet(PROTECTED_API_PATH, firstTokens.accessToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("authenticated"));

    SignInTokens secondTokens = signIn(mockMvc, objectMapper, jwtProperties, EMAIL, PASSWORD);

    assertAll(
        () -> assertNotEquals(firstTokens.accessToken(), secondTokens.accessToken()),
        () ->
            assertNotEquals(
                firstTokens.refreshToken().getValue(), secondTokens.refreshToken().getValue()),
        () -> assertEquals(1L, jwtSessionRepository.count()),
        () -> assertEquals(1L, refreshTokenRepository.count()));

    expectAccessTokenUnauthorized(mockMvc, PROTECTED_API_PATH, firstTokens.accessToken());
    expectRefreshTokenUnauthorized(mockMvc, firstTokens.refreshToken());

    mockMvc
        .perform(authenticatedGet(PROTECTED_API_PATH, secondTokens.accessToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("authenticated"));
  }

  private User saveUser() {
    User user = User.createUser("single-login-user", EMAIL, passwordEncoder.encode(PASSWORD), null);
    return userRepository.saveAndFlush(user);
  }
}
