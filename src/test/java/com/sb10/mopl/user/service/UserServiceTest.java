package com.sb10.mopl.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sb10.mopl.auth.service.AuthSessionService;
import com.sb10.mopl.auth.service.TemporaryPasswordService;
import com.sb10.mopl.common.pagination.CursorPageResponse;
import com.sb10.mopl.common.pagination.SortDirection;
import com.sb10.mopl.common.storage.ImageStorageService;
import com.sb10.mopl.user.dto.ChangePasswordRequest;
import com.sb10.mopl.user.dto.UserCreateRequest;
import com.sb10.mopl.user.dto.UserDto;
import com.sb10.mopl.user.dto.UserRoleUpdateRequest;
import com.sb10.mopl.user.dto.UserSearchRequest;
import com.sb10.mopl.user.dto.UserUpdateRequest;
import com.sb10.mopl.user.entity.User;
import com.sb10.mopl.user.entity.UserRole;
import com.sb10.mopl.user.event.UserRoleChangedEvent;
import com.sb10.mopl.user.exception.UserErrorCode;
import com.sb10.mopl.user.exception.UserException;
import com.sb10.mopl.user.mapper.UserMapper;
import com.sb10.mopl.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock private UserRepository userRepository;

  @Mock private PasswordEncoder passwordEncoder;

  @Mock private UserMapper userMapper;

  @Mock private AuthSessionService authSessionService;

  @Mock private TemporaryPasswordService temporaryPasswordService;

  @Mock private ApplicationEventPublisher eventPublisher;

  @Mock private Clock clock;

  @Mock private ImageStorageService imageStorageService;

  @InjectMocks private UserService userService;

  @Test
  @DisplayName("회원가입 시 비밀번호를 암호화하고 잠금 해제 상태의 일반 사용자를 생성한다")
  void signUp_success_whenRequestIsValid() {
    // given
    UserCreateRequest request =
        new UserCreateRequest("test-user", "user@example.com", "password123");
    UserDto expectedDto =
        new UserDto(
            UUID.randomUUID(),
            Instant.parse("2026-06-24T00:00:00Z"),
            "user@example.com",
            "test-user",
            null,
            UserRole.USER,
            false);

    when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
    when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(userMapper.toDto(any(User.class))).thenReturn(expectedDto);

    // when
    UserDto actual = userService.signUp(request);
    assertEquals(expectedDto, actual);

    // then
    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(userCaptor.capture());

    User savedUser = userCaptor.getValue();
    String savedPassword = (String) ReflectionTestUtils.getField(savedUser, "password");

    assertAll(
        () -> assertEquals("test-user", savedUser.getName()),
        () -> assertEquals("user@example.com", savedUser.getEmail()),
        () -> assertNotEquals("password123", savedPassword),
        () -> assertEquals("encoded-password", savedPassword),
        () -> assertEquals(UserRole.USER, savedUser.getRole()),
        () -> assertFalse(savedUser.isLocked()));

    verify(userRepository).existsByEmail("user@example.com");
    verify(passwordEncoder).encode("password123");
    verify(userMapper).toDto(savedUser);
  }

  @Test
  @DisplayName("이미 사용 중인 이메일이면 회원가입에 실패한다")
  void signUp_fail_whenEmailAlreadyExists() {
    // given
    UserCreateRequest request =
        new UserCreateRequest("test-user", "user@example.com", "password123");

    when(userRepository.existsByEmail("user@example.com")).thenReturn(true);

    // when
    UserException exception = assertThrows(UserException.class, () -> userService.signUp(request));

    // then
    assertAll(
        () -> assertEquals(UserErrorCode.EMAIL_ALREADY_EXISTS, exception.getErrorCode()),
        () -> assertEquals("user@example.com", exception.getDetails().get("email")));

    verify(userRepository).existsByEmail("user@example.com");
    verify(passwordEncoder, never()).encode(any());
    verify(userRepository, never()).save(any());
    verify(userMapper, never()).toDto(any());
  }

  @Test
  @DisplayName("저장 시점에 이메일 중복 제약이 발생하면 DataIntegrityViolationException이 그대로 전파된다")
  void signUp_fail_whenEmailDuplicatedDuringSave() {
    // given
    when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
    when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
    when(userRepository.save(any(User.class)))
        .thenThrow(new DataIntegrityViolationException("duplicate email"));

    UserCreateRequest request =
        new UserCreateRequest("test-user", "user@example.com", "password123");

    // when & then
    assertThrows(DataIntegrityViolationException.class, () -> userService.signUp(request));

    verify(userRepository).existsByEmail("user@example.com");
    verify(passwordEncoder).encode("password123");
    verify(userRepository).save(any(User.class));
    verify(userMapper, never()).toDto(any());
  }

  @Test
  @DisplayName("비밀번호 변경 시 새 비밀번호를 암호화해 저장하고 기존 세션을 모두 무효화한다")
  void changePassword_success_whenRequesterIsTargetUser() {
    // given
    UUID userId = UUID.randomUUID();
    User user = User.createUser("test-user", "user@example.com", "old-encoded-password", null);
    ChangePasswordRequest request = new ChangePasswordRequest("new-password");

    when(userRepository.findByIdAndIsDeletedFalse(userId)).thenReturn(Optional.of(user));
    when(passwordEncoder.encode("new-password")).thenReturn("new-encoded-password");

    // when
    userService.changePassword(userId, userId, request);

    // then
    String changedPassword = (String) ReflectionTestUtils.getField(user, "password");

    assertAll(
        () -> assertNotEquals("new-password", changedPassword),
        () -> assertEquals("new-encoded-password", changedPassword));

    verify(userRepository).findByIdAndIsDeletedFalse(userId);
    verify(passwordEncoder).encode("new-password");
    verify(temporaryPasswordService).deleteByUserId(userId);
    verify(authSessionService).invalidateAllByUserId(userId);
  }

  @Test
  @DisplayName("요청자가 본인이 아니면 비밀번호 변경에 실패한다")
  void changePassword_fail_whenRequesterIsNotTargetUser() {
    // given
    UUID targetUserId = UUID.randomUUID();
    UUID requesterUserId = UUID.randomUUID();
    ChangePasswordRequest request = new ChangePasswordRequest("new-password");

    // when
    UserException exception =
        assertThrows(
            UserException.class,
            () -> userService.changePassword(targetUserId, requesterUserId, request));

    // then
    assertAll(
        () -> assertEquals(UserErrorCode.USER_ACCESS_DENIED, exception.getErrorCode()),
        () -> assertEquals(targetUserId, exception.getDetails().get("userId")),
        () -> assertEquals(requesterUserId, exception.getDetails().get("requesterId")));

    verify(userRepository, never()).findByIdAndIsDeletedFalse(any());
    verify(passwordEncoder, never()).encode(any());
    verify(temporaryPasswordService, never()).deleteByUserId(any());
    verify(authSessionService, never()).invalidateAllByUserId(any());
  }

  @Test
  @DisplayName("존재하지 않는 사용자이면 비밀번호 변경에 실패한다")
  void changePassword_fail_whenUserDoesNotExist() {
    // given
    UUID userId = UUID.randomUUID();
    ChangePasswordRequest request = new ChangePasswordRequest("new-password");

    when(userRepository.findByIdAndIsDeletedFalse(userId)).thenReturn(Optional.empty());

    // when
    UserException exception =
        assertThrows(
            UserException.class, () -> userService.changePassword(userId, userId, request));

    // then
    assertAll(
        () -> assertEquals(UserErrorCode.USER_NOT_FOUND, exception.getErrorCode()),
        () -> assertEquals(userId, exception.getDetails().get("userId")));

    verify(userRepository).findByIdAndIsDeletedFalse(userId);
    verify(passwordEncoder, never()).encode(any());
    verify(temporaryPasswordService, never()).deleteByUserId(any());
    verify(authSessionService, never()).invalidateAllByUserId(any());
  }

  @Test
  @DisplayName("권한 변경 시 대상 사용자의 권한을 변경하고 기존 세션 무효화 후 이벤트를 발행한다")
  void updateRole_success_whenRoleChanges() {
    // given
    UUID targetUserId = UUID.randomUUID();
    UUID changedByUserId = UUID.randomUUID();
    Instant occurredAt = Instant.parse("2026-07-08T00:00:00Z");
    User user = User.createUser("test-user", "user@example.com", "encoded-password", null);
    UserRoleUpdateRequest request = new UserRoleUpdateRequest(UserRole.ADMIN);

    when(userRepository.findByIdAndIsDeletedFalse(targetUserId)).thenReturn(Optional.of(user));
    when(clock.instant()).thenReturn(occurredAt);

    // when
    userService.updateRole(targetUserId, changedByUserId, request);

    // then
    assertEquals(UserRole.ADMIN, user.getRole());

    ArgumentCaptor<UserRoleChangedEvent> eventCaptor =
        ArgumentCaptor.forClass(UserRoleChangedEvent.class);

    verify(userRepository).findByIdAndIsDeletedFalse(targetUserId);
    verify(authSessionService).invalidateAllByUserId(targetUserId);
    verify(clock).instant();
    verify(eventPublisher).publishEvent(eventCaptor.capture());

    UserRoleChangedEvent event = eventCaptor.getValue();
    assertAll(
        () -> assertEquals(targetUserId, event.targetUserId()),
        () -> assertEquals(UserRole.USER, event.previousRole()),
        () -> assertEquals(UserRole.ADMIN, event.newRole()),
        () -> assertEquals(changedByUserId, event.changedByUserId()),
        () -> assertEquals(occurredAt, event.occurredAt()));
  }

  @Test
  @DisplayName("이미 같은 권한이면 세션을 무효화하거나 이벤트를 발행하지 않는다")
  void updateRole_success_whenRoleIsSame() {
    // given
    UUID targetUserId = UUID.randomUUID();
    UUID changedByUserId = UUID.randomUUID();
    User user = User.createAdmin("test-admin", "admin@example.com", "encoded-password", null);
    UserRoleUpdateRequest request = new UserRoleUpdateRequest(UserRole.ADMIN);

    when(userRepository.findByIdAndIsDeletedFalse(targetUserId)).thenReturn(Optional.of(user));

    // when
    userService.updateRole(targetUserId, changedByUserId, request);

    // then
    assertEquals(UserRole.ADMIN, user.getRole());

    verify(userRepository).findByIdAndIsDeletedFalse(targetUserId);
    verify(authSessionService, never()).invalidateAllByUserId(any());
    verify(clock, never()).instant();
    verify(eventPublisher, never()).publishEvent(any(UserRoleChangedEvent.class));
  }

  @Test
  @DisplayName("존재하지 않는 사용자이면 권한 변경에 실패한다")
  void updateRole_fail_whenUserDoesNotExist() {
    // given
    UUID targetUserId = UUID.randomUUID();
    UUID changedByUserId = UUID.randomUUID();
    UserRoleUpdateRequest request = new UserRoleUpdateRequest(UserRole.ADMIN);

    when(userRepository.findByIdAndIsDeletedFalse(targetUserId)).thenReturn(Optional.empty());

    // when
    UserException exception =
        assertThrows(
            UserException.class,
            () -> userService.updateRole(targetUserId, changedByUserId, request));

    // then
    assertAll(
        () -> assertEquals(UserErrorCode.USER_NOT_FOUND, exception.getErrorCode()),
        () -> assertEquals(targetUserId, exception.getDetails().get("userId")));

    verify(userRepository).findByIdAndIsDeletedFalse(targetUserId);
    verify(authSessionService, never()).invalidateAllByUserId(any());
    verify(clock, never()).instant();
    verify(eventPublisher, never()).publishEvent(any(UserRoleChangedEvent.class));
  }

  @Test
  @DisplayName("사용자 목록 조회 - limit 다음 항목이 있으면 다음 커서 정보를 반환한다")
  void findUsers_success_whenNextPageExists() {
    // given
    User first =
        userWithIdentity(
            UUID.randomUUID(),
            Instant.parse("2026-06-24T00:00:00Z"),
            "alice",
            "alice@example.com",
            UserRole.USER,
            false);
    User second =
        userWithIdentity(
            UUID.randomUUID(),
            Instant.parse("2026-06-25T00:00:00Z"),
            "bob",
            "bob@example.com",
            UserRole.ADMIN,
            true);
    User lookAhead =
        userWithIdentity(
            UUID.randomUUID(),
            Instant.parse("2026-06-26T00:00:00Z"),
            "charlie",
            "charlie@example.com",
            UserRole.USER,
            false);
    UserSearchRequest request =
        new UserSearchRequest(
            null,
            null,
            null,
            null,
            null,
            2,
            SortDirection.ASCENDING,
            UserSearchRequest.SortBy.name);
    UserDto firstDto = toDto(first);
    UserDto secondDto = toDto(second);

    when(userRepository.findAllByCondition(request)).thenReturn(List.of(first, second, lookAhead));
    when(userRepository.countByCondition(request)).thenReturn(3L);
    when(userMapper.toDto(first)).thenReturn(firstDto);
    when(userMapper.toDto(second)).thenReturn(secondDto);

    // when
    CursorPageResponse<UserDto> result = userService.findUsers(request);

    // then
    assertAll(
        () -> assertThat(result.data()).containsExactly(firstDto, secondDto),
        () -> assertEquals("bob", result.nextCursor()),
        () -> assertEquals(second.getId(), result.nextIdAfter()),
        () -> assertTrue(result.hasNext()),
        () -> assertEquals(3L, result.totalCount()),
        () -> assertEquals("name", result.sortBy()),
        () -> assertEquals(SortDirection.ASCENDING, result.sortDirection()));

    verify(userRepository).findAllByCondition(request);
    verify(userRepository).countByCondition(request);
    verify(userMapper).toDto(first);
    verify(userMapper).toDto(second);
    verify(userMapper, never()).toDto(lookAhead);
  }

  @Test
  @DisplayName("사용자 목록 조회 - 다음 페이지가 없으면 다음 커서 정보를 반환하지 않는다")
  void findUsers_success_whenNextPageDoesNotExist() {
    // given
    User user =
        userWithIdentity(
            UUID.randomUUID(),
            Instant.parse("2026-06-24T00:00:00Z"),
            "alice",
            "alice@example.com",
            UserRole.USER,
            false);
    UserSearchRequest request =
        new UserSearchRequest(
            null,
            UserRole.USER,
            false,
            null,
            null,
            2,
            SortDirection.DESCENDING,
            UserSearchRequest.SortBy.createdAt);
    UserDto userDto = toDto(user);

    when(userRepository.findAllByCondition(request)).thenReturn(List.of(user));
    when(userRepository.countByCondition(request)).thenReturn(1L);
    when(userMapper.toDto(user)).thenReturn(userDto);

    // when
    CursorPageResponse<UserDto> result = userService.findUsers(request);

    // then
    assertAll(
        () -> assertThat(result.data()).containsExactly(userDto),
        () -> assertEquals(null, result.nextCursor()),
        () -> assertEquals(null, result.nextIdAfter()),
        () -> assertFalse(result.hasNext()),
        () -> assertEquals(1L, result.totalCount()),
        () -> assertEquals("createdAt", result.sortBy()),
        () -> assertEquals(SortDirection.DESCENDING, result.sortDirection()));
  }

  @Test
  @DisplayName("사용자 목록 조회 - 정렬 기준에 맞는 다음 커서를 반환한다")
  void findUsers_success_whenNextCursorUsesRequestedSortField() {
    assertNextCursorForSortBy(
        UserSearchRequest.SortBy.email,
        userWithIdentity(
            UUID.randomUUID(),
            Instant.parse("2026-06-24T00:00:00Z"),
            "email-user",
            "cursor-email@example.com",
            UserRole.USER,
            false),
        "cursor-email@example.com");
    assertNextCursorForSortBy(
        UserSearchRequest.SortBy.isLocked,
        userWithIdentity(
            UUID.randomUUID(),
            Instant.parse("2026-06-25T00:00:00Z"),
            "locked-user",
            "locked@example.com",
            UserRole.USER,
            true),
        "true");
    assertNextCursorForSortBy(
        UserSearchRequest.SortBy.role,
        userWithIdentity(
            UUID.randomUUID(),
            Instant.parse("2026-06-26T00:00:00Z"),
            "admin-user",
            "admin@example.com",
            UserRole.ADMIN,
            false),
        "ADMIN");
  }

  @Test
  @DisplayName("사용자 목록 조회 - cursor와 idAfter는 함께 전달되어야 한다")
  void findUsers_fail_whenOnlyOneCursorValueIsProvided() {
    // given
    UserSearchRequest requestWithCursorOnly =
        new UserSearchRequest(
            null,
            null,
            null,
            "alice",
            null,
            10,
            SortDirection.ASCENDING,
            UserSearchRequest.SortBy.name);
    UserSearchRequest requestWithIdAfterOnly =
        new UserSearchRequest(
            null,
            null,
            null,
            null,
            UUID.randomUUID(),
            10,
            SortDirection.ASCENDING,
            UserSearchRequest.SortBy.name);

    // when
    UserException cursorOnlyException =
        assertThrows(UserException.class, () -> userService.findUsers(requestWithCursorOnly));
    UserException idAfterOnlyException =
        assertThrows(UserException.class, () -> userService.findUsers(requestWithIdAfterOnly));

    // then
    assertAll(
        () -> assertEquals(UserErrorCode.INVALID_USER_VALUE, cursorOnlyException.getErrorCode()),
        () -> assertEquals(UserErrorCode.INVALID_USER_VALUE, idAfterOnlyException.getErrorCode()));

    verify(userRepository, never()).findAllByCondition(any());
    verify(userRepository, never()).countByCondition(any());
    verify(userMapper, never()).toDto(any());
  }

  @Test
  @DisplayName("활성 사용자를 조회하면 UserDto를 반환한다")
  void findUser_success_whenActiveUserExists() {
    // given
    UUID userId = UUID.randomUUID();
    User user =
        userWithIdentity(
            userId,
            Instant.parse("2026-06-24T00:00:00Z"),
            "test-user",
            "user@example.com",
            UserRole.USER,
            false);
    UserDto expectedDto = toDto(user);
    when(userRepository.findByIdAndIsDeletedFalse(userId)).thenReturn(Optional.of(user));
    when(userMapper.toDto(user)).thenReturn(expectedDto);

    // when
    UserDto actual = userService.findUser(userId);

    // then
    assertEquals(expectedDto, actual);
    verify(userRepository).findByIdAndIsDeletedFalse(userId);
    verify(userMapper).toDto(user);
  }

  @Test
  @DisplayName("존재하지 않거나 삭제된 사용자를 조회하면 USER_NOT_FOUND 예외를 반환한다")
  void findUser_fail_whenUserIsUnavailable() {
    // given
    UUID userId = UUID.randomUUID();
    when(userRepository.findByIdAndIsDeletedFalse(userId)).thenReturn(Optional.empty());

    // when
    UserException exception = assertThrows(UserException.class, () -> userService.findUser(userId));

    // then
    assertAll(
        () -> assertEquals(UserErrorCode.USER_NOT_FOUND, exception.getErrorCode()),
        () -> assertEquals(userId, exception.getDetails().get("userId")));
    verify(userRepository).findByIdAndIsDeletedFalse(userId);
    verify(userMapper, never()).toDto(any());
  }

  @Test
  @DisplayName("본인이 프로필 이미지와 이름을 수정하면 변경된 UserDto를 반환한다")
  void updateProfile_success_whenRequesterIsTargetUserAndImageIsProvided() {
    // given
    UUID userId = UUID.randomUUID();
    User user =
        User.createUser("old-name", "user@example.com", "encoded-password", "/uploads/old.png");
    UserUpdateRequest request = new UserUpdateRequest("new-name");
    MockMultipartFile image =
        new MockMultipartFile("image", "profile.png", "image/png", "image-bytes".getBytes());
    UserDto expectedDto =
        new UserDto(
            userId,
            Instant.parse("2026-06-24T00:00:00Z"),
            "user@example.com",
            "new-name",
            "/uploads/new.png",
            UserRole.USER,
            false);

    when(userRepository.findByIdAndIsDeletedFalse(userId)).thenReturn(Optional.of(user));
    when(imageStorageService.upload(image)).thenReturn("/uploads/new.png");
    when(userMapper.toDto(user)).thenReturn(expectedDto);

    // when
    UserDto actual = userService.updateProfile(userId, userId, request, image);

    // then
    assertAll(
        () -> assertEquals(expectedDto, actual),
        () -> assertEquals("new-name", user.getName()),
        () -> assertEquals("/uploads/new.png", user.getProfileImageUrl()));
    verify(userRepository).findByIdAndIsDeletedFalse(userId);
    verify(imageStorageService).upload(image);
    verify(userMapper).toDto(user);
  }

  @Test
  @DisplayName("프로필 이미지 없이 수정하면 기존 프로필 이미지 URL을 유지한다")
  void updateProfile_success_whenImageIsNotProvided() {
    // given
    UUID userId = UUID.randomUUID();
    User user =
        User.createUser("old-name", "user@example.com", "encoded-password", "/uploads/old.png");
    UserUpdateRequest request = new UserUpdateRequest("new-name");
    UserDto expectedDto =
        new UserDto(
            userId,
            Instant.parse("2026-06-24T00:00:00Z"),
            "user@example.com",
            "new-name",
            "/uploads/old.png",
            UserRole.USER,
            false);

    when(userRepository.findByIdAndIsDeletedFalse(userId)).thenReturn(Optional.of(user));
    when(userMapper.toDto(user)).thenReturn(expectedDto);

    // when
    UserDto actual = userService.updateProfile(userId, userId, request, null);

    // then
    assertAll(
        () -> assertEquals(expectedDto, actual),
        () -> assertEquals("new-name", user.getName()),
        () -> assertEquals("/uploads/old.png", user.getProfileImageUrl()));
    verify(userRepository).findByIdAndIsDeletedFalse(userId);
    verify(imageStorageService, never()).upload(any());
    verify(userMapper).toDto(user);
  }

  @Test
  @DisplayName("업로드 결과가 비어 있으면 기존 프로필 이미지 URL을 유지한다")
  void updateProfile_success_whenImageUploadResultIsBlank() {
    // given
    UUID userId = UUID.randomUUID();
    User user =
        User.createUser("old-name", "user@example.com", "encoded-password", "/uploads/old.png");
    UserUpdateRequest request = new UserUpdateRequest("new-name");
    MockMultipartFile image =
        new MockMultipartFile("image", "profile.png", "image/png", "image-bytes".getBytes());

    when(userRepository.findByIdAndIsDeletedFalse(userId)).thenReturn(Optional.of(user));
    when(imageStorageService.upload(image)).thenReturn(" ");

    // when
    userService.updateProfile(userId, userId, request, image);

    // then
    assertEquals("/uploads/old.png", user.getProfileImageUrl());
    verify(imageStorageService).upload(image);
  }

  @Test
  @DisplayName("다른 사용자의 프로필 수정 요청은 사용자 조회 전에 거부한다")
  void updateProfile_fail_whenRequesterIsNotTargetUser() {
    // given
    UUID targetUserId = UUID.randomUUID();
    UUID requesterUserId = UUID.randomUUID();
    UserUpdateRequest request = new UserUpdateRequest("new-name");

    // when
    UserException exception =
        assertThrows(
            UserException.class,
            () -> userService.updateProfile(targetUserId, requesterUserId, request, null));

    // then
    assertAll(
        () -> assertEquals(UserErrorCode.USER_ACCESS_DENIED, exception.getErrorCode()),
        () -> assertEquals(targetUserId, exception.getDetails().get("userId")),
        () -> assertEquals(requesterUserId, exception.getDetails().get("requesterId")));
    verify(userRepository, never()).findByIdAndIsDeletedFalse(any());
    verify(imageStorageService, never()).upload(any());
  }

  @Test
  @DisplayName("존재하지 않는 사용자의 프로필 수정 요청은 USER_NOT_FOUND 예외를 반환한다")
  void updateProfile_fail_whenUserDoesNotExist() {
    // given
    UUID userId = UUID.randomUUID();
    UserUpdateRequest request = new UserUpdateRequest("new-name");
    when(userRepository.findByIdAndIsDeletedFalse(userId)).thenReturn(Optional.empty());

    // when
    UserException exception =
        assertThrows(
            UserException.class, () -> userService.updateProfile(userId, userId, request, null));

    // then
    assertAll(
        () -> assertEquals(UserErrorCode.USER_NOT_FOUND, exception.getErrorCode()),
        () -> assertEquals(userId, exception.getDetails().get("userId")));
    verify(userRepository).findByIdAndIsDeletedFalse(userId);
    verify(imageStorageService, never()).upload(any());
    verify(userMapper, never()).toDto(any());
  }

  private User userWithIdentity(
      UUID id, Instant createdAt, String name, String email, UserRole role, boolean locked) {
    User user =
        role == UserRole.ADMIN
            ? User.createAdmin(name, email, "encoded-password", null)
            : User.createUser(name, email, "encoded-password", null);
    user.changeLocked(locked);
    ReflectionTestUtils.setField(user, "id", id);
    ReflectionTestUtils.setField(user, "createdAt", createdAt);
    return user;
  }

  private void assertNextCursorForSortBy(
      UserSearchRequest.SortBy sortBy, User pageUser, String expectedNextCursor) {
    User lookAhead =
        userWithIdentity(
            UUID.randomUUID(),
            pageUser.getCreatedAt().plusSeconds(1),
            pageUser.getName() + "-next",
            "next-" + pageUser.getEmail(),
            UserRole.USER,
            false);
    UserSearchRequest request =
        new UserSearchRequest(null, null, null, null, null, 1, SortDirection.ASCENDING, sortBy);
    UserDto pageUserDto = toDto(pageUser);

    when(userRepository.findAllByCondition(request)).thenReturn(List.of(pageUser, lookAhead));
    when(userRepository.countByCondition(request)).thenReturn(2L);
    when(userMapper.toDto(pageUser)).thenReturn(pageUserDto);

    CursorPageResponse<UserDto> result = userService.findUsers(request);

    assertAll(
        () -> assertThat(result.data()).containsExactly(pageUserDto),
        () -> assertEquals(expectedNextCursor, result.nextCursor()),
        () -> assertEquals(pageUser.getId(), result.nextIdAfter()),
        () -> assertTrue(result.hasNext()),
        () -> assertEquals(sortBy.name(), result.sortBy()));
  }

  private UserDto toDto(User user) {
    return new UserDto(
        user.getId(),
        user.getCreatedAt(),
        user.getEmail(),
        user.getName(),
        user.getProfileImageUrl(),
        user.getRole(),
        user.isLocked());
  }
}
