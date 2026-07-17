package com.sb10.mopl.user.controller;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sb10.mopl.auth.security.user.AuthenticatedUser;
import com.sb10.mopl.auth.security.user.CurrentUserArgumentResolver;
import com.sb10.mopl.common.exception.GlobalExceptionHandler;
import com.sb10.mopl.common.pagination.CursorPageResponse;
import com.sb10.mopl.common.pagination.SortDirection;
import com.sb10.mopl.config.WebMvcConfig;
import com.sb10.mopl.content.dto.ContentSummary;
import com.sb10.mopl.content.entity.ContentType;
import com.sb10.mopl.user.dto.request.ChangePasswordRequest;
import com.sb10.mopl.user.dto.request.UserCreateRequest;
import com.sb10.mopl.user.dto.request.UserRoleUpdateRequest;
import com.sb10.mopl.user.dto.request.UserSearchRequest;
import com.sb10.mopl.user.dto.request.UserUpdateRequest;
import com.sb10.mopl.user.dto.response.UserDto;
import com.sb10.mopl.user.dto.response.UserSummary;
import com.sb10.mopl.user.entity.UserRole;
import com.sb10.mopl.user.exception.UserErrorCode;
import com.sb10.mopl.user.exception.UserException;
import com.sb10.mopl.user.service.UserService;
import java.nio.charset.StandardCharsets;
import com.sb10.mopl.watchingsession.dto.WatchingSessionDto;
import com.sb10.mopl.watchingsession.service.WatchingSessionService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@Import({GlobalExceptionHandler.class, WebMvcConfig.class, CurrentUserArgumentResolver.class})
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private UserService userService;

  @MockitoBean private WatchingSessionService watchingSessionService;

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

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

  @Test
  @DisplayName("사용자 프로필 조회 요청이 유효하면 200 OK와 UserDto를 반환한다")
  void findUser_success_whenUserExists() throws Exception {
    // given
    UUID userId = UUID.randomUUID();
    UserDto userDto =
        new UserDto(
            userId,
            Instant.parse("2026-06-24T00:00:00Z"),
            "user@example.com",
            "test-user",
            "/uploads/profile.png",
            UserRole.USER,
            false);
    when(userService.findUser(userId)).thenReturn(userDto);

    // when & then
    mockMvc
        .perform(get("/api/users/{userId}", userId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(userId.toString()))
        .andExpect(jsonPath("$.createdAt").value("2026-06-24T00:00:00Z"))
        .andExpect(jsonPath("$.email").value("user@example.com"))
        .andExpect(jsonPath("$.name").value("test-user"))
        .andExpect(jsonPath("$.profileImageUrl").value("/uploads/profile.png"))
        .andExpect(jsonPath("$.role").value("USER"))
        .andExpect(jsonPath("$.locked").value(false))
        .andExpect(jsonPath("$.password").doesNotExist());

    verify(userService).findUser(userId);
  }

  @Test
  @DisplayName("존재하지 않는 사용자 프로필 조회 요청은 404 Not Found를 반환한다")
  void findUser_fail_whenUserDoesNotExist() throws Exception {
    // given
    UUID userId = UUID.randomUUID();
    when(userService.findUser(userId))
        .thenThrow(new UserException(UserErrorCode.USER_NOT_FOUND, Map.of("userId", userId)));

    // when & then
    mockMvc
        .perform(get("/api/users/{userId}", userId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("U01"));

    verify(userService).findUser(userId);
  }

  @Test
  @DisplayName("사용자 프로필 조회 요청의 userId가 UUID 형식이 아니면 400 Bad Request를 반환한다")
  void findUser_fail_whenUserIdIsInvalid() throws Exception {
    // when & then
    mockMvc
        .perform(get("/api/users/{userId}", "invalid-user-id"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("SYS01"));

    verifyNoInteractions(userService);
  }

  @Test
  @DisplayName("프로필 수정 요청이 유효하면 200 OK와 수정된 UserDto를 반환한다")
  void updateProfile_success_whenRequestIsValid() throws Exception {
    // given
    UUID userId = UUID.randomUUID();
    authenticate(userId);
    UserUpdateRequest request = new UserUpdateRequest("updated-user");
    UserDto userDto =
        new UserDto(
            userId,
            Instant.parse("2026-06-24T00:00:00Z"),
            "user@example.com",
            "updated-user",
            "/uploads/profile.png",
            UserRole.USER,
            false);
    MockMultipartFile image =
        new MockMultipartFile("image", "profile.png", "image/png", "image-bytes".getBytes());
    when(userService.updateProfile(eq(userId), eq(userId), any(UserUpdateRequest.class), any()))
        .thenReturn(userDto);

    // when & then
    mockMvc
        .perform(
            multipart("/api/users/{userId}", userId)
                .file(requestPart(request))
                .file(image)
                .with(
                    servletRequest -> {
                      servletRequest.setMethod("PATCH");
                      return servletRequest;
                    })
                .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(userId.toString()))
        .andExpect(jsonPath("$.name").value("updated-user"))
        .andExpect(jsonPath("$.profileImageUrl").value("/uploads/profile.png"));

    ArgumentCaptor<UserUpdateRequest> requestCaptor =
        ArgumentCaptor.forClass(UserUpdateRequest.class);
    verify(userService).updateProfile(eq(userId), eq(userId), requestCaptor.capture(), eq(image));
    assertEquals("updated-user", requestCaptor.getValue().name());
  }

  @Test
  @DisplayName("이미지 없이 프로필을 수정하면 200 OK를 반환한다")
  void updateProfile_success_whenImageIsNotProvided() throws Exception {
    // given
    UUID userId = UUID.randomUUID();
    authenticate(userId);
    UserUpdateRequest request = new UserUpdateRequest("updated-user");
    UserDto userDto =
        new UserDto(
            userId,
            Instant.parse("2026-06-24T00:00:00Z"),
            "user@example.com",
            "updated-user",
            "/uploads/old-profile.png",
            UserRole.USER,
            false);
    when(userService.updateProfile(eq(userId), eq(userId), any(UserUpdateRequest.class), isNull()))
        .thenReturn(userDto);

    // when & then
    mockMvc
        .perform(
            multipart("/api/users/{userId}", userId)
                .file(requestPart(request))
                .with(
                    servletRequest -> {
                      servletRequest.setMethod("PATCH");
                      return servletRequest;
                    })
                .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("updated-user"))
        .andExpect(jsonPath("$.profileImageUrl").value("/uploads/old-profile.png"));

    verify(userService)
        .updateProfile(eq(userId), eq(userId), any(UserUpdateRequest.class), isNull());
  }

  @Test
  @DisplayName("다른 사용자의 프로필 수정 요청은 403 Forbidden을 반환한다")
  void updateProfile_fail_whenRequesterIsNotTargetUser() throws Exception {
    // given
    UUID targetUserId = UUID.randomUUID();
    UUID requesterUserId = UUID.randomUUID();
    authenticate(requesterUserId);
    UserUpdateRequest request = new UserUpdateRequest("updated-user");
    doThrow(
            new UserException(
                UserErrorCode.USER_ACCESS_DENIED,
                Map.of("userId", targetUserId, "requesterId", requesterUserId)))
        .when(userService)
        .updateProfile(
            eq(targetUserId), eq(requesterUserId), any(UserUpdateRequest.class), isNull());

    // when & then
    mockMvc
        .perform(
            multipart("/api/users/{userId}", targetUserId)
                .file(requestPart(request))
                .with(
                    servletRequest -> {
                      servletRequest.setMethod("PATCH");
                      return servletRequest;
                    })
                .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("U03"));

    verify(userService)
        .updateProfile(
            eq(targetUserId), eq(requesterUserId), any(UserUpdateRequest.class), isNull());
  }

  @Test
  @DisplayName("프로필 수정 요청의 이름이 유효하지 않으면 400 Bad Request를 반환한다")
  void updateProfile_fail_whenNameIsInvalid() throws Exception {
    // given
    UUID userId = UUID.randomUUID();
    authenticate(userId);

    // when & then
    mockMvc
        .perform(
            multipart("/api/users/{userId}", userId)
                .file(requestPart(new UserUpdateRequest("")))
                .with(
                    servletRequest -> {
                      servletRequest.setMethod("PATCH");
                      return servletRequest;
                    })
                .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("SYS01"));

    verifyNoInteractions(userService);
  }

  @Test
  @DisplayName("프로필 수정 요청에 request part가 없으면 400 Bad Request를 반환한다")
  void updateProfile_fail_whenRequestPartIsMissing() throws Exception {
    // given
    UUID userId = UUID.randomUUID();
    authenticate(userId);

    // when & then
    mockMvc
        .perform(
            multipart("/api/users/{userId}", userId)
                .with(
                    servletRequest -> {
                      servletRequest.setMethod("PATCH");
                      return servletRequest;
                    })
                .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("SYS01"));

    verifyNoInteractions(userService);
  }

  @Test
  @DisplayName("프로필 수정 요청의 userId가 UUID 형식이 아니면 400 Bad Request를 반환한다")
  void updateProfile_fail_whenUserIdIsInvalid() throws Exception {
    // given
    UUID requesterUserId = UUID.randomUUID();
    authenticate(requesterUserId);

    // when & then
    mockMvc
        .perform(
            multipart("/api/users/{userId}", "invalid-user-id")
                .file(requestPart(new UserUpdateRequest("updated-user")))
                .with(
                    servletRequest -> {
                      servletRequest.setMethod("PATCH");
                      return servletRequest;
                    })
                .contentType(MediaType.MULTIPART_FORM_DATA))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("SYS01"));

    verifyNoInteractions(userService);
  }

  @Test
  @DisplayName("비밀번호 변경 요청이 유효하면 204 No Content를 반환한다")
  void changePassword_success_whenRequestIsValid() throws Exception {
    // given
    UUID userId = UUID.randomUUID();
    authenticate(userId);
    Map<String, String> request = Map.of("password", "new-password");

    // when & then
    mockMvc
        .perform(
            patch("/api/users/{userId}/password", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNoContent());

    ArgumentCaptor<ChangePasswordRequest> captor =
        ArgumentCaptor.forClass(ChangePasswordRequest.class);
    verify(userService).changePassword(eq(userId), eq(userId), captor.capture());

    assertEquals("new-password", captor.getValue().password());
  }

  @Test
  @DisplayName("요청자가 본인이 아니면 비밀번호 변경 요청에 403 Forbidden을 반환한다")
  void changePassword_fail_whenRequesterIsNotTargetUser() throws Exception {
    // given
    UUID targetUserId = UUID.randomUUID();
    UUID requesterUserId = UUID.randomUUID();
    authenticate(requesterUserId);
    Map<String, String> request = Map.of("password", "new-password");

    doThrow(
            new UserException(
                UserErrorCode.USER_ACCESS_DENIED,
                Map.of("userId", targetUserId, "requesterId", requesterUserId)))
        .when(userService)
        .changePassword(eq(targetUserId), eq(requesterUserId), any(ChangePasswordRequest.class));

    // when & then
    mockMvc
        .perform(
            patch("/api/users/{userId}/password", targetUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());

    verify(userService)
        .changePassword(eq(targetUserId), eq(requesterUserId), any(ChangePasswordRequest.class));
  }

  @Test
  @DisplayName("존재하지 않는 사용자이면 비밀번호 변경 요청에 404 Not Found를 반환한다")
  void changePassword_fail_whenUserDoesNotExist() throws Exception {
    // given
    UUID userId = UUID.randomUUID();
    authenticate(userId);
    Map<String, String> request = Map.of("password", "new-password");

    doThrow(new UserException(UserErrorCode.USER_NOT_FOUND, Map.of("userId", userId)))
        .when(userService)
        .changePassword(eq(userId), eq(userId), any(ChangePasswordRequest.class));

    // when & then
    mockMvc
        .perform(
            patch("/api/users/{userId}/password", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound());

    verify(userService).changePassword(eq(userId), eq(userId), any(ChangePasswordRequest.class));
  }

  @Test
  @DisplayName("새 비밀번호가 유효하지 않으면 400 Bad Request를 반환한다")
  void changePassword_fail_whenPasswordIsInvalid() throws Exception {
    // given
    UUID userId = UUID.randomUUID();
    authenticate(userId);
    Map<String, String> request = Map.of("password", "short");

    // when & then
    mockMvc
        .perform(
            patch("/api/users/{userId}/password", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(userService);
  }

  @Test
  @DisplayName("권한 변경 요청이 유효하면 204 No Content를 반환한다")
  void updateRole_success_whenRequestIsValid() throws Exception {
    // given
    UUID targetUserId = UUID.randomUUID();
    UUID requesterUserId = UUID.randomUUID();
    authenticate(requesterUserId, UserRole.ADMIN);
    Map<String, String> request = Map.of("role", "ADMIN");

    // when & then
    mockMvc
        .perform(
            patch("/api/users/{userId}/role", targetUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNoContent());

    ArgumentCaptor<UserRoleUpdateRequest> captor =
        ArgumentCaptor.forClass(UserRoleUpdateRequest.class);
    verify(userService).updateRole(eq(targetUserId), eq(requesterUserId), captor.capture());

    assertEquals(UserRole.ADMIN, captor.getValue().role());
  }

  @Test
  @DisplayName("존재하지 않는 사용자이면 권한 변경 요청에 404 Not Found를 반환한다")
  void updateRole_fail_whenUserDoesNotExist() throws Exception {
    // given
    UUID targetUserId = UUID.randomUUID();
    UUID requesterUserId = UUID.randomUUID();
    authenticate(requesterUserId, UserRole.ADMIN);
    Map<String, String> request = Map.of("role", "ADMIN");

    doThrow(new UserException(UserErrorCode.USER_NOT_FOUND, Map.of("userId", targetUserId)))
        .when(userService)
        .updateRole(eq(targetUserId), eq(requesterUserId), any(UserRoleUpdateRequest.class));

    // when & then
    mockMvc
        .perform(
            patch("/api/users/{userId}/role", targetUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound());

    verify(userService)
        .updateRole(eq(targetUserId), eq(requesterUserId), any(UserRoleUpdateRequest.class));
  }

  @Test
  @DisplayName("권한 변경 요청에서 role 값이 지원하지 않는 값이면 400 Bad Request를 반환한다")
  void updateRole_fail_whenRoleIsInvalid() throws Exception {
    // given
    authenticate(UUID.randomUUID(), UserRole.ADMIN);
    Map<String, String> request = Map.of("role", "MANAGER");

    // when & then
    mockMvc
        .perform(
            patch("/api/users/{userId}/role", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(userService);
  }

  @Test
  @DisplayName("권한 변경 요청에서 role이 누락되면 400 Bad Request를 반환한다")
  void updateRole_fail_whenRoleIsMissing() throws Exception {
    // given
    authenticate(UUID.randomUUID(), UserRole.ADMIN);
    Map<String, String> request = Map.of();

    // when & then
    mockMvc
        .perform(
            patch("/api/users/{userId}/role", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(userService);
  }

  @Test
  @DisplayName("사용자 목록 조회 요청이 유효하면 200 OK와 CursorPageResponse를 반환한다")
  void findUsers_success_whenRequestIsValid() throws Exception {
    // given
    UUID firstUserId = UUID.randomUUID();
    UUID requestIdAfter = UUID.randomUUID();
    UUID responseNextIdAfter = UUID.randomUUID();
    UserDto firstUser =
        new UserDto(
            firstUserId,
            Instant.parse("2026-06-24T00:00:00Z"),
            "locked-user@example.com",
            "locked-user",
            null,
            UserRole.USER,
            true);
    CursorPageResponse<UserDto> response =
        new CursorPageResponse<>(
            List.of(firstUser),
            "locked-user",
            responseNextIdAfter,
            true,
            3L,
            "name",
            SortDirection.ASCENDING);

    when(userService.findUsers(any(UserSearchRequest.class))).thenReturn(response);

    // when & then
    mockMvc
        .perform(
            get("/api/users")
                .param("emailLike", "USER")
                .param("roleEqual", "USER")
                .param("isLocked", "true")
                .param("cursor", "alice")
                .param("idAfter", requestIdAfter.toString())
                .param("limit", "10")
                .param("sortBy", "name")
                .param("sortDirection", "ASCENDING"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].id").value(firstUserId.toString()))
        .andExpect(jsonPath("$.data[0].createdAt").value("2026-06-24T00:00:00Z"))
        .andExpect(jsonPath("$.data[0].email").value("locked-user@example.com"))
        .andExpect(jsonPath("$.data[0].name").value("locked-user"))
        .andExpect(jsonPath("$.data[0].profileImageUrl").doesNotExist())
        .andExpect(jsonPath("$.data[0].role").value("USER"))
        .andExpect(jsonPath("$.data[0].locked").value(true))
        .andExpect(jsonPath("$.data[0].password").doesNotExist())
        .andExpect(jsonPath("$.nextCursor").value("locked-user"))
        .andExpect(jsonPath("$.nextIdAfter").value(responseNextIdAfter.toString()))
        .andExpect(jsonPath("$.hasNext").value(true))
        .andExpect(jsonPath("$.totalCount").value(3))
        .andExpect(jsonPath("$.sortBy").value("name"))
        .andExpect(jsonPath("$.sortDirection").value("ASCENDING"));

    ArgumentCaptor<UserSearchRequest> captor = ArgumentCaptor.forClass(UserSearchRequest.class);
    verify(userService).findUsers(captor.capture());
    UserSearchRequest capturedRequest = captor.getValue();

    assertAll(
        () -> assertEquals("USER", capturedRequest.emailLike()),
        () -> assertEquals(UserRole.USER, capturedRequest.roleEqual()),
        () -> assertEquals(true, capturedRequest.isLocked()),
        () -> assertEquals("alice", capturedRequest.cursor()),
        () -> assertEquals(requestIdAfter, capturedRequest.idAfter()),
        () -> assertEquals(10, capturedRequest.limit()),
        () -> assertEquals(SortDirection.ASCENDING, capturedRequest.sortDirection()),
        () -> assertEquals(UserSearchRequest.SortBy.name, capturedRequest.sortBy()));
  }

  @Test
  @DisplayName("사용자 목록 조회 요청에서 limit이 누락되면 400 Bad Request를 반환한다")
  void findUsers_fail_whenLimitIsMissing() throws Exception {
    // when & then
    mockMvc
        .perform(get("/api/users").param("sortBy", "name").param("sortDirection", "ASCENDING"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("SYS01"));

    verifyNoInteractions(userService);
  }

  @Test
  @DisplayName("사용자 목록 조회 요청에서 sortBy가 지원하지 않는 값이면 400 Bad Request를 반환한다")
  void findUsers_fail_whenSortByIsInvalid() throws Exception {
    // when & then
    mockMvc
        .perform(
            get("/api/users")
                .param("limit", "10")
                .param("sortBy", "unsupported")
                .param("sortDirection", "ASCENDING"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("SYS01"));

    verifyNoInteractions(userService);
  }

  @Test
  @DisplayName("사용자 목록 조회 요청에서 sortDirection이 지원하지 않는 값이면 400 Bad Request를 반환한다")
  void findUsers_fail_whenSortDirectionIsInvalid() throws Exception {
    // when & then
    mockMvc
        .perform(
            get("/api/users")
                .param("limit", "10")
                .param("sortBy", "name")
                .param("sortDirection", "INVALID"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("SYS01"));

    verifyNoInteractions(userService);
  }

  @Test
  @DisplayName("사용자 목록 조회 요청에서 limit이 100을 초과하면 400 Bad Request를 반환한다")
  void findUsers_fail_whenLimitExceedsMaximum() throws Exception {
    // when & then
    mockMvc
        .perform(
            get("/api/users")
                .param("limit", "101")
                .param("sortBy", "name")
                .param("sortDirection", "ASCENDING"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("SYS01"));

    verifyNoInteractions(userService);
  }

  @Test
  @DisplayName("시청 중인 콘텐츠가 있으면 200 OK와 WatchingSessionDto를 반환한다")
  void findWatchingSession_success_whenActiveSessionExists() throws Exception {
    // given
    UUID watcherId = UUID.randomUUID();
    UUID sessionId = UUID.randomUUID();
    UUID contentId = UUID.randomUUID();
    WatchingSessionDto dto =
        new WatchingSessionDto(
            sessionId,
            Instant.parse("2026-07-16T00:00:00Z"),
            new UserSummary(watcherId, "test-watcher", null),
            new ContentSummary(
                contentId,
                ContentType.MOVIE,
                "test-content",
                "설명",
                "https://example.com/thumbnail.jpg",
                List.of("action", "thriller"),
                4.5,
                10));

    when(watchingSessionService.findLatestByWatcher(watcherId)).thenReturn(Optional.of(dto));

    // when & then
    mockMvc
        .perform(get("/api/users/{watcherId}/watching-sessions", watcherId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(sessionId.toString()))
        .andExpect(jsonPath("$.createdAt").value("2026-07-16T00:00:00Z"))
        .andExpect(jsonPath("$.watcher.userId").value(watcherId.toString()))
        .andExpect(jsonPath("$.watcher.name").value("test-watcher"))
        .andExpect(jsonPath("$.watcher.profileImageUrl").doesNotExist())
        .andExpect(jsonPath("$.content.id").value(contentId.toString()))
        .andExpect(jsonPath("$.content.type").value("movie"))
        .andExpect(jsonPath("$.content.title").value("test-content"))
        .andExpect(jsonPath("$.content.averageRating").value(4.5))
        .andExpect(jsonPath("$.content.reviewCount").value(10));

    verify(watchingSessionService).findLatestByWatcher(watcherId);
  }

  @Test
  @DisplayName("시청 중인 콘텐츠가 없으면 404 Not Found를 반환한다")
  void findWatchingSession_fail_whenNoActiveSession() throws Exception {
    // given
    UUID watcherId = UUID.randomUUID();
    when(watchingSessionService.findLatestByWatcher(watcherId)).thenReturn(Optional.empty());

    // when & then
    mockMvc
        .perform(get("/api/users/{watcherId}/watching-sessions", watcherId))
        .andExpect(status().isNotFound());

    verify(watchingSessionService).findLatestByWatcher(watcherId);
  }

  private void authenticate(UUID userId) {
    authenticate(userId, UserRole.USER);
  }

  private void authenticate(UUID userId, UserRole role) {
    AuthenticatedUser currentUser = new AuthenticatedUser(userId, "current-user@example.com", role);
    SecurityContextHolder.getContext()
        .setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(
                currentUser, null, List.of(new SimpleGrantedAuthority(role.authorityName()))));
  }

  private MockMultipartFile requestPart(UserUpdateRequest request) throws Exception {
    return new MockMultipartFile(
        "request",
        "",
        MediaType.APPLICATION_JSON_VALUE,
        objectMapper.writeValueAsString(request).getBytes(StandardCharsets.UTF_8));
  }
}
