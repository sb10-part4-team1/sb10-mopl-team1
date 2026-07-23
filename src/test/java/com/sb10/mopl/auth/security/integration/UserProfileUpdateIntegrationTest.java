package com.sb10.mopl.auth.security.integration;

import static com.sb10.mopl.auth.security.integration.AuthIntegrationTestSupport.signIn;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sb10.mopl.auth.repository.JwtSessionRepository;
import com.sb10.mopl.auth.repository.RefreshTokenRepository;
import com.sb10.mopl.auth.security.integration.AuthIntegrationTestSupport.SignInTokens;
import com.sb10.mopl.auth.security.jwt.JwtProperties;
import com.sb10.mopl.user.dto.UserUpdateRequest;
import com.sb10.mopl.user.entity.User;
import com.sb10.mopl.user.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserProfileUpdateIntegrationTest {

  private static final String EMAIL = "profile-update@example.com";
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
  @DisplayName("비로그인 사용자는 프로필 수정 API 호출 시 401을 받는다")
  void updateProfile_returnsUnauthorized_whenAnonymousUserRequests() throws Exception {
    User user = saveUser("original-name");

    mockMvc
        .perform(
            multipart("/api/users/{userId}", user.getId())
                .file(requestPart(new UserUpdateRequest("updated-name")))
                .with(
                    servletRequest -> {
                      servletRequest.setMethod("PATCH");
                      return servletRequest;
                    })
                .with(csrf()))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTH01"));
  }

  @Test
  @DisplayName("로그인한 사용자는 CSRF 토큰과 함께 자신의 프로필을 수정할 수 있다")
  void updateProfile_updatesOwnProfile_whenAuthenticatedWithCsrfToken() throws Exception {
    User user = saveUser("original-name");
    SignInTokens tokens = signIn(mockMvc, objectMapper, jwtProperties, EMAIL, PASSWORD);

    mockMvc
        .perform(
            multipart("/api/users/{userId}", user.getId())
                .file(requestPart(new UserUpdateRequest("updated-name")))
                .with(
                    servletRequest -> {
                      servletRequest.setMethod("PATCH");
                      return servletRequest;
                    })
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.accessToken())
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(user.getId().toString()))
        .andExpect(jsonPath("$.name").value("updated-name"));

    User updatedUser = userRepository.findById(user.getId()).orElseThrow();
    assertEquals("updated-name", updatedUser.getName());
  }

  @Test
  @DisplayName("로그인한 사용자가 CSRF 토큰 없이 프로필을 수정하면 403을 반환한다")
  void updateProfile_returnsForbidden_whenCsrfTokenIsMissing() throws Exception {
    User user = saveUser("original-name");
    SignInTokens tokens = signIn(mockMvc, objectMapper, jwtProperties, EMAIL, PASSWORD);

    mockMvc
        .perform(
            multipart("/api/users/{userId}", user.getId())
                .file(requestPart(new UserUpdateRequest("updated-name")))
                .with(
                    servletRequest -> {
                      servletRequest.setMethod("PATCH");
                      return servletRequest;
                    })
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.accessToken()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("SYS04"));

    User unchangedUser = userRepository.findById(user.getId()).orElseThrow();
    assertEquals("original-name", unchangedUser.getName());
  }

  private User saveUser(String name) {
    User user =
        User.createUser(name, EMAIL, passwordEncoder.encode(PASSWORD), "/uploads/profile.png");
    return userRepository.saveAndFlush(user);
  }

  private MockMultipartFile requestPart(UserUpdateRequest request) throws Exception {
    return new MockMultipartFile(
        "request",
        "",
        MediaType.APPLICATION_JSON_VALUE,
        objectMapper.writeValueAsString(request).getBytes(StandardCharsets.UTF_8));
  }
}
