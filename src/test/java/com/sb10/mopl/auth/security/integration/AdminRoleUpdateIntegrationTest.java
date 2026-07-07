package com.sb10.mopl.auth.security.integration;

import static com.sb10.mopl.auth.security.integration.AuthIntegrationTestSupport.PROTECTED_API_PATH;
import static com.sb10.mopl.auth.security.integration.AuthIntegrationTestSupport.authenticatedGet;
import static com.sb10.mopl.auth.security.integration.AuthIntegrationTestSupport.expectAccessTokenUnauthorized;
import static com.sb10.mopl.auth.security.integration.AuthIntegrationTestSupport.expectRefreshTokenUnauthorized;
import static com.sb10.mopl.auth.security.integration.AuthIntegrationTestSupport.signIn;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sb10.mopl.auth.repository.JwtSessionRepository;
import com.sb10.mopl.auth.repository.RefreshTokenRepository;
import com.sb10.mopl.auth.security.integration.AuthIntegrationTestSupport.SignInTokens;
import com.sb10.mopl.auth.security.jwt.JwtProperties;
import com.sb10.mopl.user.entity.User;
import com.sb10.mopl.user.entity.UserRole;
import com.sb10.mopl.user.repository.UserRepository;
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
class AdminRoleUpdateIntegrationTest {

  private static final String PASSWORD = "password123";
  private static final String TARGET_EMAIL = "role-target@example.com";
  private static final String ADMIN_EMAIL = "role-admin@example.com";
  private static final String USER_EMAIL = "role-user@example.com";

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
  @DisplayName("비로그인 사용자는 권한 변경 API 호출 시 401을 받는다")
  void updateRole_returnsUnauthorized_whenAnonymousUserRequests() throws Exception {
    User targetUser = saveUser(UserRole.USER, TARGET_EMAIL);

    mockMvc
        .perform(
            patch("/api/users/{userId}/role", targetUser.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(roleRequest(UserRole.ADMIN))
                .with(csrf()))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTH01"));
  }

  @Test
  @DisplayName("일반 사용자는 권한 변경 API 호출 시 403을 받는다")
  void updateRole_returnsForbidden_whenUserRequests() throws Exception {
    User targetUser = saveUser(UserRole.USER, TARGET_EMAIL);
    saveUser(UserRole.USER, USER_EMAIL);
    SignInTokens userTokens = signIn(mockMvc, objectMapper, jwtProperties, USER_EMAIL, PASSWORD);

    mockMvc
        .perform(
            patch("/api/users/{userId}/role", targetUser.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userTokens.accessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(roleRequest(UserRole.ADMIN))
                .with(csrf()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("SYS04"));
  }

  @Test
  @DisplayName("관리자가 권한을 변경하면 대상 사용자 권한과 인증 상태가 정리된다")
  void updateRole_updatesRoleAndInvalidatesTargetTokens_whenAdminRequests() throws Exception {
    User targetUser = saveUser(UserRole.USER, TARGET_EMAIL);
    saveUser(UserRole.ADMIN, ADMIN_EMAIL);
    SignInTokens targetTokens =
        signIn(mockMvc, objectMapper, jwtProperties, TARGET_EMAIL, PASSWORD);
    SignInTokens adminTokens = signIn(mockMvc, objectMapper, jwtProperties, ADMIN_EMAIL, PASSWORD);

    mockMvc
        .perform(authenticatedGet(PROTECTED_API_PATH, targetTokens.accessToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("authenticated"));

    mockMvc
        .perform(
            patch("/api/users/{userId}/role", targetUser.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminTokens.accessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(roleRequest(UserRole.ADMIN))
                .with(csrf()))
        .andExpect(status().isNoContent());

    User updatedTargetUser = userRepository.findById(targetUser.getId()).orElseThrow();

    assertAll(
        () -> assertEquals(UserRole.ADMIN, updatedTargetUser.getRole()),
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
  @DisplayName("관리자는 자기 자신의 권한을 변경할 수 있고 기존 인증 상태가 정리된다")
  void updateRole_updatesOwnRoleAndInvalidatesOwnTokens_whenAdminRequestsSelf() throws Exception {
    User adminUser = saveUser(UserRole.ADMIN, ADMIN_EMAIL);
    SignInTokens adminTokens = signIn(mockMvc, objectMapper, jwtProperties, ADMIN_EMAIL, PASSWORD);

    mockMvc
        .perform(authenticatedGet(PROTECTED_API_PATH, adminTokens.accessToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("authenticated"));

    mockMvc
        .perform(
            patch("/api/users/{userId}/role", adminUser.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminTokens.accessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(roleRequest(UserRole.USER))
                .with(csrf()))
        .andExpect(status().isNoContent());

    User updatedAdminUser = userRepository.findById(adminUser.getId()).orElseThrow();

    assertAll(
        () -> assertEquals(UserRole.USER, updatedAdminUser.getRole()),
        () -> assertEquals(0L, jwtSessionRepository.count()),
        () -> assertEquals(0L, refreshTokenRepository.count()));

    expectAccessTokenUnauthorized(mockMvc, PROTECTED_API_PATH, adminTokens.accessToken());
    expectRefreshTokenUnauthorized(mockMvc, adminTokens.refreshToken());
  }

  private User saveUser(UserRole role, String email) {
    User user =
        role == UserRole.ADMIN
            ? User.createAdmin(
                role.name().toLowerCase(), email, passwordEncoder.encode(PASSWORD), null)
            : User.createUser(
                role.name().toLowerCase(), email, passwordEncoder.encode(PASSWORD), null);
    return userRepository.saveAndFlush(user);
  }

  private String roleRequest(UserRole role) throws Exception {
    return objectMapper.writeValueAsString(Map.of("role", role.name()));
  }
}
