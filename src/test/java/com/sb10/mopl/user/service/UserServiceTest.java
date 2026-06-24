package com.sb10.mopl.user.service;

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

import com.sb10.mopl.user.dto.request.UserCreateRequest;
import com.sb10.mopl.user.dto.response.UserDto;
import com.sb10.mopl.user.entity.User;
import com.sb10.mopl.user.entity.UserRole;
import com.sb10.mopl.user.exception.UserErrorCode;
import com.sb10.mopl.user.exception.UserException;
import com.sb10.mopl.user.mapper.UserMapper;
import com.sb10.mopl.user.repository.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock private UserRepository userRepository;

  @Mock private PasswordEncoder passwordEncoder;

  @Mock private UserMapper userMapper;

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
    when(userRepository.saveAndFlush(any(User.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(userMapper.toDto(any(User.class))).thenReturn(expectedDto);

    // when
    UserDto actual = userService.signUp(request);
    assertEquals(expectedDto, actual);

    // then
    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).saveAndFlush(userCaptor.capture());

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
    verify(userRepository, never()).saveAndFlush(any());
    verify(userMapper, never()).toDto(any());
  }

  @Test
  @DisplayName("저장 중 이메일 중복 제약이 발생하면 사용자 예외로 변환한다")
  void signUp_fail_whenEmailDuplicatedDuringSave() {
    // given
    UserCreateRequest request =
        new UserCreateRequest("test-user", "user@example.com", "password123");

    when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
    when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
    when(userRepository.saveAndFlush(any(User.class)))
        .thenThrow(new DataIntegrityViolationException("duplicate email"));

    // when
    UserException exception = assertThrows(UserException.class, () -> userService.signUp(request));

    // then
    assertAll(
        () -> assertEquals(UserErrorCode.EMAIL_ALREADY_EXISTS, exception.getErrorCode()),
        () -> assertEquals("user@example.com", exception.getDetails().get("email")),
        () -> assertTrue(exception.getCause() instanceof DataIntegrityViolationException));

    verify(userRepository).existsByEmail("user@example.com");
    verify(passwordEncoder).encode("password123");
    verify(userRepository).saveAndFlush(any(User.class));
    verify(userMapper, never()).toDto(any());
  }
}
