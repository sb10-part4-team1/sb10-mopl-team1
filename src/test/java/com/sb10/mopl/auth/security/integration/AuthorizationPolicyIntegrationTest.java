package com.sb10.mopl.auth.security.integration;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sb10.mopl.auth.security.jwt.JwtProvider;
import com.sb10.mopl.auth.security.user.MoplUserDetails;
import com.sb10.mopl.user.entity.User;
import com.sb10.mopl.user.entity.UserRole;
import com.sb10.mopl.user.repository.UserRepository;
import java.util.UUID;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(AuthorizationPolicyIntegrationTest.AuthorizationPolicyTestController.class)
class AuthorizationPolicyIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private JwtProvider jwtProvider;

  @Autowired private PasswordEncoder passwordEncoder;

  @Autowired private UserRepository userRepository;

  @BeforeEach
  void setUp() {
    userRepository.deleteAll();
  }

  @Test
  @DisplayName("비로그인 사용자는 공개 API에 접근할 수 있다")
  void publicApi_returnsOk_whenAnonymousUserRequestsApiDocsEndpoint() throws Exception {
    mockMvc
        .perform(get("/api-docs/test-public"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("public api docs"));
  }

  @Test
  @DisplayName("비로그인 사용자는 보호 API 호출 시 401을 받는다")
  void protectedApi_returnsUnauthorized_whenAnonymousUserRequestsProtectedEndpoint()
      throws Exception {
    mockMvc
        .perform(get("/api/test/protected"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTH01"));
  }

  @Test
  @DisplayName("일반 사용자는 ADMIN API 호출 시 403을 받는다")
  void adminApi_returnsForbidden_whenUserRequestsAdminEndpoint() throws Exception {
    String accessToken = accessToken(saveUser(UserRole.USER, "user@example.com"));

    mockMvc
        .perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("SYS04"));
  }

  @Test
  @DisplayName("일반 사용자는 권한 변경 API 호출 시 403을 받는다")
  void rolePatch_returnsForbidden_whenUserRequestsAdminRoleEndpoint() throws Exception {
    String accessToken = accessToken(saveUser(UserRole.USER, "user-role@example.com"));
    UUID userId = UUID.randomUUID();

    mockMvc
        .perform(
            patch("/api/users/{userId}/role", userId)
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                .with(csrf()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("SYS04"));
  }

  @Test
  @DisplayName("비로그인 사용자는 계정 잠금 API 호출 시 401을 받는다")
  void lockedPatch_returnsUnauthorized_whenAnonymousUserRequestsLockedEndpoint() throws Exception {
    mockMvc
        .perform(patch("/api/users/{userId}/locked", UUID.randomUUID()).with(csrf()))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTH01"));
  }

  @Test
  @DisplayName("관리자는 사용자 목록 조회, 권한 변경, 계정 잠금 API에 접근할 수 있다")
  void adminApi_returnsOk_whenAdminRequestsUserManagementEndpoints() throws Exception {
    String accessToken = accessToken(saveUser(UserRole.ADMIN, "admin@example.com"));
    UUID userId = UUID.randomUUID();

    mockMvc
        .perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("admin users"));

    mockMvc
        .perform(
            patch("/api/users/{userId}/role", userId)
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("admin role"));

    mockMvc
        .perform(
            patch("/api/users/{userId}/locked", userId)
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("admin locked"));
  }

  @Test
  @DisplayName("권한 매핑은 ROLE_USER, ROLE_ADMIN 형태로 동작한다")
  void authorityMapping_returnsRoleAuthorities_whenUserDetailsCreated() {
    MoplUserDetails userDetails =
        new MoplUserDetails(saveUser(UserRole.USER, "role-user@example.com"));
    MoplUserDetails adminDetails =
        new MoplUserDetails(saveUser(UserRole.ADMIN, "role-admin@example.com"));

    assertAll(
        () -> assertEquals("ROLE_USER", UserRole.USER.authorityName()),
        () -> assertEquals("ROLE_ADMIN", UserRole.ADMIN.authorityName()),
        () ->
            org.hamcrest.MatcherAssert.assertThat(
                userDetails.getAuthorities().stream()
                    .map(authority -> authority.getAuthority())
                    .toList(),
                containsInAnyOrder("ROLE_USER")),
        () ->
            org.hamcrest.MatcherAssert.assertThat(
                adminDetails.getAuthorities().stream()
                    .map(authority -> authority.getAuthority())
                    .toList(),
                containsInAnyOrder("ROLE_ADMIN")));
  }

  private User saveUser(UserRole role, String email) {
    User user =
        role == UserRole.ADMIN
            ? User.createAdmin("test-admin", email, passwordEncoder.encode("password123"), null)
            : User.createUser("test-user", email, passwordEncoder.encode("password123"), null);
    return userRepository.saveAndFlush(user);
  }

  private String accessToken(User user) {
    return jwtProvider.createAccessToken(new MoplUserDetails(user));
  }

  private String bearer(String accessToken) {
    return "Bearer " + accessToken;
  }

  @RestController
  static class AuthorizationPolicyTestController {

    @GetMapping("/api/test/protected")
    MessageResponse protectedApi() {
      return new MessageResponse("protected");
    }

    @GetMapping("/api-docs/test-public")
    MessageResponse publicApiDocs() {
      return new MessageResponse("public api docs");
    }

    @GetMapping("/api/users")
    MessageResponse users() {
      return new MessageResponse("admin users");
    }

    @PatchMapping("/api/users/{userId}/role")
    MessageResponse role() {
      return new MessageResponse("admin role");
    }

    @PatchMapping("/api/users/{userId}/locked")
    MessageResponse locked() {
      return new MessageResponse("admin locked");
    }
  }

  record MessageResponse(String message) {}
}
