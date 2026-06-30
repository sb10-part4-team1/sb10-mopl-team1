package com.sb10.mopl.auth.security.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sb10.mopl.user.entity.User;
import com.sb10.mopl.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@Import(CsrfTokenIntegrationTest.CsrfPolicyTestController.class)
class CsrfTokenIntegrationTest {

  private static final String EMAIL = "csrf-user@example.com";
  private static final String PASSWORD = "password123";

  @Autowired private MockMvc mockMvc;

  @Autowired private UserRepository userRepository;

  @Autowired private PasswordEncoder passwordEncoder;

  @BeforeEach
  void setUp() {
    SecurityContextHolder.clearContext();
    userRepository.deleteAll();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("CSRF 토큰 조회 시 XSRF-TOKEN 쿠키를 발급한다")
  void getCsrfToken_setsXsrfTokenCookie() throws Exception {
    Cookie xsrfCookie = getXsrfTokenCookie();

    assertNotNull(xsrfCookie);
    assertFalse(xsrfCookie.getValue().isBlank());
    assertEquals("/", xsrfCookie.getPath());
  }

  @Test
  @DisplayName("로그인 요청에 CSRF 헤더가 없으면 403을 반환한다")
  void signIn_returnsForbidden_whenCsrfHeaderIsMissing() throws Exception {
    saveUser();
    Cookie xsrfCookie = getXsrfTokenCookie();

    mockMvc
        .perform(
            post("/api/auth/sign-in")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("username", EMAIL)
                .param("password", PASSWORD)
                .cookie(xsrfCookie))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("SYS04"));
  }

  @Test
  @DisplayName("XSRF-TOKEN 쿠키 값을 X-XSRF-TOKEN 헤더로 보내면 로그인에 성공한다")
  void signIn_success_whenXsrfCookieValueIsSentAsHeader() throws Exception {
    saveUser();
    Cookie xsrfCookie = getXsrfTokenCookie();

    mockMvc
        .perform(
            post("/api/auth/sign-in")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("username", EMAIL)
                .param("password", PASSWORD)
                .cookie(xsrfCookie)
                .header("X-XSRF-TOKEN", xsrfCookie.getValue()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isString())
        .andExpect(jsonPath("$.userDto.email").value(EMAIL));
  }

  @Test
  @DisplayName("잘못된 X-XSRF-TOKEN 헤더로 로그인 요청하면 403을 반환한다")
  void signIn_returnsForbidden_whenCsrfHeaderIsInvalid() throws Exception {
    saveUser();
    Cookie xsrfCookie = getXsrfTokenCookie();

    mockMvc
        .perform(
            post("/api/auth/sign-in")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("username", EMAIL)
                .param("password", PASSWORD)
                .cookie(xsrfCookie)
                .header("X-XSRF-TOKEN", "invalid-csrf-token"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("SYS04"));
  }

  @Test
  @DisplayName("올바른 X-XSRF-TOKEN 헤더가 있으면 POST 요청을 통과시킨다")
  void postRequest_success_whenXsrfCookieValueIsSentAsHeader() throws Exception {
    Cookie xsrfCookie = getXsrfTokenCookie();

    mockMvc
        .perform(
            post("/api/test/csrf")
                .with(user("csrf-user"))
                .cookie(xsrfCookie)
                .header("X-XSRF-TOKEN", xsrfCookie.getValue()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("posted"));
  }

  @Test
  @DisplayName("올바른 X-XSRF-TOKEN 헤더가 있으면 PATCH 요청을 통과시킨다")
  void patchRequest_success_whenXsrfCookieValueIsSentAsHeader() throws Exception {
    Cookie xsrfCookie = getXsrfTokenCookie();

    mockMvc
        .perform(
            patch("/api/test/csrf")
                .with(user("csrf-user"))
                .cookie(xsrfCookie)
                .header("X-XSRF-TOKEN", xsrfCookie.getValue()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("patched"));
  }

  @Test
  @DisplayName("올바른 X-XSRF-TOKEN 헤더가 있으면 DELETE 요청을 통과시킨다")
  void deleteRequest_success_whenXsrfCookieValueIsSentAsHeader() throws Exception {
    Cookie xsrfCookie = getXsrfTokenCookie();

    mockMvc
        .perform(
            delete("/api/test/csrf")
                .with(user("csrf-user"))
                .cookie(xsrfCookie)
                .header("X-XSRF-TOKEN", xsrfCookie.getValue()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("deleted"));
  }

  @Test
  @DisplayName("리프레시 토큰 재발급 요청에 CSRF 헤더가 없으면 403을 반환한다")
  void refresh_returnsForbidden_whenCsrfHeaderIsMissing() throws Exception {
    Cookie xsrfCookie = getXsrfTokenCookie();

    mockMvc
        .perform(post("/api/auth/refresh").cookie(xsrfCookie))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("SYS04"));
  }

  @Test
  @DisplayName("로그아웃 요청에 CSRF 헤더가 없으면 403을 반환한다")
  void signOut_returnsForbidden_whenCsrfHeaderIsMissing() throws Exception {
    Cookie xsrfCookie = getXsrfTokenCookie();

    mockMvc
        .perform(post("/api/auth/sign-out").cookie(xsrfCookie))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("SYS04"));
  }

  private Cookie getXsrfTokenCookie() throws Exception {
    MvcResult result =
        mockMvc.perform(get("/api/auth/csrf-token")).andExpect(status().isNoContent()).andReturn();
    return result.getResponse().getCookie("XSRF-TOKEN");
  }

  private User saveUser() {
    User user = User.createUser("csrf-user", EMAIL, passwordEncoder.encode(PASSWORD), null);
    return userRepository.saveAndFlush(user);
  }

  @RestController
  static class CsrfPolicyTestController {

    @PostMapping("/api/test/csrf")
    MessageResponse post() {
      return new MessageResponse("posted");
    }

    @PatchMapping("/api/test/csrf")
    MessageResponse patch() {
      return new MessageResponse("patched");
    }

    @DeleteMapping("/api/test/csrf")
    MessageResponse delete() {
      return new MessageResponse("deleted");
    }
  }

  record MessageResponse(String message) {}
}
