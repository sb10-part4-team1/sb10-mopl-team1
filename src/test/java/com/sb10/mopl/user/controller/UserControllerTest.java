package com.sb10.mopl.user.controller;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sb10.mopl.common.exception.GlobalExceptionHandler;
import com.sb10.mopl.user.dto.request.UserCreateRequest;
import com.sb10.mopl.user.dto.response.UserDto;
import com.sb10.mopl.user.entity.UserRole;
import com.sb10.mopl.user.exception.UserErrorCode;
import com.sb10.mopl.user.exception.UserException;
import com.sb10.mopl.user.service.UserService;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private UserService userService;

  @Test
  @DisplayName("회원가입 요청이 유효하면 201 Created와 UserDto를 반환한다")
  void signUp_success_whenRequestIsValid() throws Exception {
    // given
    UUID userId = UUID.randomUUID();
    UserDto userDto =
        new UserDto(
            userId,
            Instant.parse("2026-06-24T00:00:00Z"),
            "user@example.com",
            "test-user",
            null,
            UserRole.USER,
            false);
    Map<String, String> request =
        Map.of("name", "test-user", "email", "user@example.com", "password", "password123");

    when(userService.signUp(any(UserCreateRequest.class))).thenReturn(userDto);

    // when & then
    mockMvc
        .perform(
            post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/users/" + userId))
        .andExpect(jsonPath("$.id").value(userId.toString()))
        .andExpect(jsonPath("$.createdAt").value("2026-06-24T00:00:00Z"))
        .andExpect(jsonPath("$.email").value("user@example.com"))
        .andExpect(jsonPath("$.name").value("test-user"))
        .andExpect(jsonPath("$.profileImageUrl").doesNotExist())
        .andExpect(jsonPath("$.role").value("USER"))
        .andExpect(jsonPath("$.locked").value(false))
        .andExpect(jsonPath("$.password").doesNotExist());

    ArgumentCaptor<UserCreateRequest> captor = ArgumentCaptor.forClass(UserCreateRequest.class);
    verify(userService).signUp(captor.capture());
    UserCreateRequest capturedRequest = captor.getValue();

    assertAll(
        () -> assertEquals("test-user", capturedRequest.name()),
        () -> assertEquals("user@example.com", capturedRequest.email()),
        () -> assertEquals("password123", capturedRequest.password()));
  }

  @Test
  @DisplayName("이미 사용 중인 이메일이면 409 Conflict를 반환한다")
  void signUp_fail_whenEmailAlreadyExists() throws Exception {
    // given
    Map<String, String> request =
        Map.of("name", "test-user", "email", "user@example.com", "password", "password123");

    when(userService.signUp(any(UserCreateRequest.class)))
        .thenThrow(
            new UserException(
                UserErrorCode.EMAIL_ALREADY_EXISTS, Map.of("email", "user@example.com")));

    // when & then
    mockMvc
        .perform(
            post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict());

    verify(userService).signUp(any(UserCreateRequest.class));
  }

  @Test
  @DisplayName("이름이 유효하지 않으면 400 Bad Request를 반환한다")
  void signUp_fail_whenNameIsInvalid() throws Exception {
    // given
    Map<String, String> request =
        Map.of("name", "", "email", "user@example.com", "password", "password123");

    // when & then
    mockMvc
        .perform(
            post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(userService);
  }

  @Test
  @DisplayName("이메일이 유효하지 않으면 400 Bad Request를 반환한다")
  void signUp_fail_whenEmailIsInvalid() throws Exception {
    // given
    Map<String, String> request =
        Map.of("name", "test-user", "email", "invalid-email", "password", "password123");

    // when & then
    mockMvc
        .perform(
            post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(userService);
  }

  @Test
  @DisplayName("비밀번호가 유효하지 않으면 400 Bad Request를 반환한다")
  void signUp_fail_whenPasswordIsInvalid() throws Exception {
    // given
    Map<String, String> request =
        Map.of("name", "test-user", "email", "user@example.com", "password", "short");

    // when & then
    mockMvc
        .perform(
            post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(userService);
  }
}
