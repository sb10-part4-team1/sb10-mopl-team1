package com.sb10.mopl.auth.security.filter;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sb10.mopl.auth.security.jwt.JwtProvider;
import com.sb10.mopl.user.entity.User;
import com.sb10.mopl.user.entity.UserRole;
import com.sb10.mopl.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
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
class EmailPasswordAuthenticationFilterIntegrationTest {

  private static final String EMAIL = "login-user@example.com";
  private static final String PASSWORD = "password123";

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private UserRepository userRepository;

  @Autowired private PasswordEncoder passwordEncoder;

  @Autowired private JwtProvider jwtProvider;

  @BeforeEach
  void setUp() {
    userRepository.deleteAll();
  }

  @Test
  @DisplayName("로그인 요청이 유효하면 토큰 응답 객체와 접근 토큰을 반환한다")
  void signIn_success_whenCredentialsAreValid() throws Exception {
    User user = saveUser();

    MvcResult result =
        mockMvc
            .perform(
                post("/api/auth/sign-in")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .param("username", EMAIL)
                    .param("password", PASSWORD)
                    .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
            .andExpect(header().string(HttpHeaders.PRAGMA, "no-cache"))
            .andExpect(jsonPath("$.userDto.id").value(user.getId().toString()))
            .andExpect(jsonPath("$.userDto.email").value(EMAIL))
            .andExpect(jsonPath("$.userDto.name").value("login-user"))
            .andExpect(jsonPath("$.userDto.role").value("USER"))
            .andExpect(jsonPath("$.userDto.locked").value(false))
            .andExpect(jsonPath("$.accessToken").isString())
            .andReturn();

    JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
    String accessToken = response.get("accessToken").asText();
    Claims claims = jwtProvider.parseClaims(accessToken);

    assertAll(
        () -> assertEquals(user.getId().toString(), claims.getSubject()),
        () -> assertEquals(user.getId().toString(), claims.get("id")),
        () -> assertEquals(EMAIL, claims.get("email")),
        () -> assertEquals(UserRole.USER.name(), claims.get("role")),
        () -> assertEquals(JwtProvider.ACCESS_TOKEN_TYPE, claims.get(JwtProvider.TOKEN_TYPE_CLAIM)),
        () -> assertNotNull(claims.getIssuedAt()),
        () -> assertNotNull(claims.getExpiration()),
        () -> assertTrue(claims.getExpiration().after(claims.getIssuedAt())));
  }

  @Test
  @DisplayName("비밀번호가 일치하지 않으면 401 인증 실패를 반환한다")
  void signIn_fail_whenPasswordDoesNotMatch() throws Exception {
    saveUser();

    mockMvc
        .perform(
            post("/api/auth/sign-in")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("username", EMAIL)
                .param("password", "wrong-password")
                .with(csrf()))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTH01"));
  }

  @Test
  @DisplayName("존재하지 않는 이메일로 로그인하면 401 인증 실패를 반환한다")
  void signIn_fail_whenEmailDoesNotExist() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/sign-in")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("username", "missing@example.com")
                .param("password", PASSWORD)
                .with(csrf()))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTH01"));
  }

  @Test
  @DisplayName("로그인 요청 형식이 폼 인코딩이 아니면 400 잘못된 요청을 반환한다")
  void signIn_fail_whenContentTypeIsNotFormUrlEncoded() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/sign-in")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"login-user@example.com\",\"password\":\"password123\"}")
                .with(csrf()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("SYS01"))
        .andExpect(jsonPath("$.details.contentType").exists());
  }

  private User saveUser() {
    User user =
        User.createUser("login-user", EMAIL, passwordEncoder.encode(PASSWORD), "/profiles/me.png");
    return userRepository.saveAndFlush(user);
  }
}
