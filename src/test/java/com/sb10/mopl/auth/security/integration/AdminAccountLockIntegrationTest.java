package com.sb10.mopl.auth.security.integration;

import static com.sb10.mopl.auth.security.integration.AuthIntegrationTestSupport.PROTECTED_API_PATH;
import static com.sb10.mopl.auth.security.integration.AuthIntegrationTestSupport.authenticatedGet;
import static com.sb10.mopl.auth.security.integration.AuthIntegrationTestSupport.expectAccessTokenUnauthorized;
import static com.sb10.mopl.auth.security.integration.AuthIntegrationTestSupport.expectRefreshTokenUnauthorized;
import static com.sb10.mopl.auth.security.integration.AuthIntegrationTestSupport.signIn;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import com.sb10.mopl.user.entity.UserRole;
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
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(AuthIntegrationTestSupport.ProtectedApiController.class)
class AdminAccountLockIntegrationTest {

  private static final String PASSWORD = "password123";
  private static final String TEMPORARY_PASSWORD = "Temp1234!";
  private static final String TARGET_EMAIL = "lock-target@example.com";
  private static final String ADMIN_EMAIL = "lock-admin@example.com";
  private static final String USER_EMAIL = "lock-user@example.com";

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
  @DisplayName("비로그인 사용자는 계정 잠금 API 호출 시 401을 받는다")
  void updateLocked_returnsUnauthorized_whenAnonymousUserRequests() throws Exception {
    User targetUser = saveUser(UserRole.USER, TARGET_EMAIL, false);

    mockMvc
        .perform(
            patch("/api/users/{userId}/locked", targetUser.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(lockRequest(true))
                .with(csrf()))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTH01"));
  }

  @Test
  @DisplayName("일반 사용자는 계정 잠금 API 호출 시 403을 받는다")
  void updateLocked_returnsForbidden_whenUserRequests() throws Exception {
    User targetUser = saveUser(UserRole.USER, TARGET_EMAIL, false);
    saveUser(UserRole.USER, USER_EMAIL, false);
    SignInTokens userTokens = signIn(mockMvc, objectMapper, jwtProperties, USER_EMAIL, PASSWORD);

    mockMvc
        .perform(
            patch("/api/users/{userId}/locked", targetUser.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userTokens.accessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(lockRequest(true))
                .with(csrf()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("SYS04"));
  }

  @Test
  @DisplayName("관리자가 계정을 잠그면 204를 반환하고 대상 사용자의 인증 상태를 정리한다")
  void updateLocked_locksUserAndInvalidatesTargetTokens_whenAdminRequests() throws Exception {
    User targetUser = saveUser(UserRole.USER, TARGET_EMAIL, false);
    saveUser(UserRole.ADMIN, ADMIN_EMAIL, false);
    SignInTokens targetTokens =
        signIn(mockMvc, objectMapper, jwtProperties, TARGET_EMAIL, PASSWORD);
    SignInTokens adminTokens = signIn(mockMvc, objectMapper, jwtProperties, ADMIN_EMAIL, PASSWORD);

    mockMvc
        .perform(authenticatedGet(PROTECTED_API_PATH, targetTokens.accessToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("authenticated"));

    mockMvc
        .perform(
            patch("/api/users/{userId}/locked", targetUser.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminTokens.accessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(lockRequest(true))
                .with(csrf()))
        .andExpect(status().isNoContent());

    User updatedTargetUser = userRepository.findById(targetUser.getId()).orElseThrow();

    assertAll(
        () -> assertTrue(updatedTargetUser.isLocked()),
        () -> assertEquals(1L, jwtSessionRepository.count()),
        () -> assertEquals(1L, refreshTokenRepository.count()));

    expectAccessTokenUnauthorized(mockMvc, PROTECTED_API_PATH, targetTokens.accessToken());
    expectRefreshTokenUnauthorized(mockMvc, targetTokens.refreshToken());

    mockMvc
        .perform(authenticatedGet(PROTECTED_API_PATH, adminTokens.accessToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("authenticated"));
  }

  @Test
  @DisplayName("관리자가 계정 잠금을 해제하면 204를 반환하고 대상 사용자가 다시 로그인할 수 있다")
  void updateLocked_unlocksUserAndAllowsSignIn_whenAdminRequests() throws Exception {
    User targetUser = saveUser(UserRole.USER, TARGET_EMAIL, true);
    saveUser(UserRole.ADMIN, ADMIN_EMAIL, false);
    SignInTokens adminTokens = signIn(mockMvc, objectMapper, jwtProperties, ADMIN_EMAIL, PASSWORD);

    mockMvc
        .perform(
            patch("/api/users/{userId}/locked", targetUser.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminTokens.accessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(lockRequest(false))
                .with(csrf()))
        .andExpect(status().isNoContent());

    User updatedTargetUser = userRepository.findById(targetUser.getId()).orElseThrow();
    assertFalse(updatedTargetUser.isLocked());

    mockMvc
        .perform(
            post("/api/auth/sign-in")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("username", TARGET_EMAIL)
                .param("password", PASSWORD)
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userDto.id").value(targetUser.getId().toString()))
        .andExpect(jsonPath("$.accessToken").isString());
  }

  @Test
  @DisplayName("잠긴 계정은 이메일과 비밀번호로 로그인할 수 없다")
  void signIn_returnsUnauthorized_whenUserIsLocked() throws Exception {
    saveUser(UserRole.USER, TARGET_EMAIL, true);

    mockMvc
        .perform(
            post("/api/auth/sign-in")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("username", TARGET_EMAIL)
                .param("password", PASSWORD)
                .with(csrf()))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTH01"));

    assertAll(
        () -> assertEquals(0L, jwtSessionRepository.count()),
        () -> assertEquals(0L, refreshTokenRepository.count()));
  }

  @Test
  @DisplayName("잠긴 계정은 유효한 임시 비밀번호로도 로그인할 수 없다")
  void signIn_returnsUnauthorized_whenTemporaryPasswordUserIsLocked() throws Exception {
    User targetUser = saveUser(UserRole.USER, TARGET_EMAIL, true);
    saveTemporaryPassword(targetUser, Duration.ofMinutes(3));

    mockMvc
        .perform(
            post("/api/auth/sign-in")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("username", TARGET_EMAIL)
                .param("password", TEMPORARY_PASSWORD)
                .with(csrf()))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTH01"));

    assertAll(
        () -> assertEquals(0L, jwtSessionRepository.count()),
        () -> assertEquals(0L, refreshTokenRepository.count()));
  }

  @Test
  @DisplayName("잠긴 계정은 저장된 리프레시 토큰이 있어도 토큰을 재발급할 수 없다")
  void refresh_returnsUnauthorized_whenUserIsLocked() throws Exception {
    User targetUser = saveUser(UserRole.USER, TARGET_EMAIL, false);
    SignInTokens targetTokens =
        signIn(mockMvc, objectMapper, jwtProperties, TARGET_EMAIL, PASSWORD);

    targetUser.changeLocked(true);
    userRepository.saveAndFlush(targetUser);

    expectRefreshTokenUnauthorized(mockMvc, targetTokens.refreshToken());
  }

  @Test
  @DisplayName("잠금 상태 요청값이 없으면 400을 반환한다")
  void updateLocked_returnsBadRequest_whenLockedValueIsMissing() throws Exception {
    User targetUser = saveUser(UserRole.USER, TARGET_EMAIL, false);
    saveUser(UserRole.ADMIN, ADMIN_EMAIL, false);
    SignInTokens adminTokens = signIn(mockMvc, objectMapper, jwtProperties, ADMIN_EMAIL, PASSWORD);

    mockMvc
        .perform(
            patch("/api/users/{userId}/locked", targetUser.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminTokens.accessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"locked\":null}")
                .with(csrf()))
        .andExpect(status().isBadRequest());
  }

  private User saveUser(UserRole role, String email, boolean locked) {
    User user =
        role == UserRole.ADMIN
            ? User.createAdmin(
                role.name().toLowerCase(), email, passwordEncoder.encode(PASSWORD), null)
            : User.createUser(
                role.name().toLowerCase(), email, passwordEncoder.encode(PASSWORD), null);
    user.changeLocked(locked);
    return userRepository.saveAndFlush(user);
  }

  private void saveTemporaryPassword(User user, Duration expiresIn) {
    temporaryPasswordRepository.saveAndFlush(
        TemporaryPassword.create(
            user,
            passwordEncoder.encode(AdminAccountLockIntegrationTest.TEMPORARY_PASSWORD),
            clock.instant().plus(expiresIn)));
  }

  private String lockRequest(boolean locked) throws Exception {
    return objectMapper.writeValueAsString(Map.of("locked", locked));
  }
}
