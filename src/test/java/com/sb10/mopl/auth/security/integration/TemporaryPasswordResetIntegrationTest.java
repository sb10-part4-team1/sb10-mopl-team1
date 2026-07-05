package com.sb10.mopl.auth.security.integration;

import static com.sb10.mopl.auth.security.integration.AuthIntegrationTestSupport.signIn;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sb10.mopl.auth.entity.TemporaryPassword;
import com.sb10.mopl.auth.repository.JwtSessionRepository;
import com.sb10.mopl.auth.repository.RefreshTokenRepository;
import com.sb10.mopl.auth.repository.TemporaryPasswordRepository;
import com.sb10.mopl.auth.security.integration.AuthIntegrationTestSupport.SignInTokens;
import com.sb10.mopl.auth.security.jwt.JwtProperties;
import com.sb10.mopl.user.entity.User;
import com.sb10.mopl.user.repository.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
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
class TemporaryPasswordResetIntegrationTest {

  private static final String EMAIL = "temporary-password-user@example.com";
  private static final String PASSWORD = "password123";
  private static final String TEMPORARY_PASSWORD = "Temp1234!";

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private UserRepository userRepository;

  @Autowired private TemporaryPasswordRepository temporaryPasswordRepository;

  @Autowired private RefreshTokenRepository refreshTokenRepository;

  @Autowired private JwtSessionRepository jwtSessionRepository;

  @Autowired private PasswordEncoder passwordEncoder;

  @Autowired private JwtProperties jwtProperties;

  @Autowired private Clock clock;

  @BeforeEach
  void setUp() {
    temporaryPasswordRepository.deleteAll();
    refreshTokenRepository.deleteAll();
    jwtSessionRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  @DisplayName("임시 비밀번호 초기화 요청이 성공하면 204를 반환하고 임시 비밀번호를 저장한다")
  void resetPassword_success_whenUserExists() throws Exception {
    saveUser();

    mockMvc
        .perform(
            post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", EMAIL)))
                .with(csrf()))
        .andExpect(status().isNoContent());

    assertEquals(1L, temporaryPasswordRepository.count());
  }

  @Test
  @DisplayName("존재하지 않는 이메일로 초기화를 요청해도 204를 반환하고 임시 비밀번호를 만들지 않는다")
  void resetPassword_returnsNoContent_whenUserDoesNotExist() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", "unknown@example.com")))
                .with(csrf()))
        .andExpect(status().isNoContent());

    assertEquals(0L, temporaryPasswordRepository.count());
  }

  @Test
  @DisplayName("유효한 임시 비밀번호로 로그인할 수 있다")
  void signIn_success_whenTemporaryPasswordIsValid() throws Exception {
    User user = saveUser();
    saveTemporaryPassword(user, TEMPORARY_PASSWORD, Duration.ofMinutes(3));

    MvcResult result =
        mockMvc
            .perform(
                post("/api/auth/sign-in")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .param("username", EMAIL)
                    .param("password", TEMPORARY_PASSWORD)
                    .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userDto.id").value(user.getId().toString()))
            .andExpect(jsonPath("$.userDto.email").value(EMAIL))
            .andExpect(jsonPath("$.accessToken").isString())
            .andReturn();

    assertAll(
        () -> assertNotNull(result.getResponse().getCookie(refreshTokenCookieName())),
        () -> assertEquals(1L, jwtSessionRepository.count()),
        () -> assertEquals(1L, refreshTokenRepository.count()));
  }

  @Test
  @DisplayName("만료된 임시 비밀번호로 로그인할 수 없다")
  void signIn_fail_whenTemporaryPasswordIsExpired() throws Exception {
    User user = saveUser();
    saveTemporaryPassword(user, TEMPORARY_PASSWORD, Duration.ofSeconds(-1));

    mockMvc
        .perform(
            post("/api/auth/sign-in")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("username", EMAIL)
                .param("password", TEMPORARY_PASSWORD)
                .with(csrf()))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTH01"));
  }

  @Test
  @DisplayName("비밀번호 변경 후 발급된 임시 비밀번호는 파기되어 다시 로그인할 수 없다")
  void changePassword_deletesTemporaryPasswordAndPreventsReuse() throws Exception {
    User user = saveUser();
    saveTemporaryPassword(user, TEMPORARY_PASSWORD, Duration.ofMinutes(3));
    SignInTokens temporaryPasswordTokens =
        signIn(mockMvc, objectMapper, jwtProperties, EMAIL, TEMPORARY_PASSWORD);

    mockMvc
        .perform(
            patch("/api/users/{userId}/password", user.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + temporaryPasswordTokens.accessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("password", "new-password")))
                .with(csrf()))
        .andExpect(status().isNoContent());

    assertEquals(0L, temporaryPasswordRepository.count());

    mockMvc
        .perform(
            post("/api/auth/sign-in")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("username", EMAIL)
                .param("password", TEMPORARY_PASSWORD)
                .with(csrf()))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTH01"));

    User updatedUser = userRepository.findById(user.getId()).orElseThrow();
    assertTrue(passwordEncoder.matches("new-password", updatedUser.passwordHash()));
  }

  private User saveUser() {
    User user =
        User.createUser("temporary-password-user", EMAIL, passwordEncoder.encode(PASSWORD), null);
    return userRepository.saveAndFlush(user);
  }

  private TemporaryPassword saveTemporaryPassword(
      User user, String temporaryPassword, Duration expiresIn) {
    return temporaryPasswordRepository.saveAndFlush(
        TemporaryPassword.create(
            user, passwordEncoder.encode(temporaryPassword), clock.instant().plus(expiresIn)));
  }

  private String refreshTokenCookieName() {
    return jwtProperties.refreshTokenCookie().name();
  }
}
