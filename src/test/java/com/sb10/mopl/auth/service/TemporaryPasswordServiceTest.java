package com.sb10.mopl.auth.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sb10.mopl.auth.entity.TemporaryPassword;
import com.sb10.mopl.auth.event.TemporaryPasswordIssuedEvent;
import com.sb10.mopl.auth.repository.TemporaryPasswordRepository;
import com.sb10.mopl.auth.util.TemporaryPasswordGenerator;
import com.sb10.mopl.user.entity.User;
import com.sb10.mopl.user.exception.UserErrorCode;
import com.sb10.mopl.user.exception.UserException;
import com.sb10.mopl.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TemporaryPasswordServiceTest {

  private static final Instant FIXED_NOW = Instant.parse("2026-07-05T00:00:00Z");

  @Mock private TemporaryPasswordRepository temporaryPasswordRepository;

  @Mock private UserRepository userRepository;

  @Mock private PasswordEncoder passwordEncoder;

  @Mock private TemporaryPasswordGenerator temporaryPasswordGenerator;

  @Mock private ApplicationEventPublisher eventPublisher;

  private TemporaryPasswordService temporaryPasswordService;

  @BeforeEach
  void setUp() {
    Clock fixedClock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
    temporaryPasswordService =
        new TemporaryPasswordService(
            temporaryPasswordRepository,
            userRepository,
            passwordEncoder,
            temporaryPasswordGenerator,
            eventPublisher,
            fixedClock);
  }

  @Test
  @DisplayName("임시 비밀번호를 발급할 때 기존 발급분을 삭제하고 암호화된 비밀번호와 3분 만료 시간을 저장한다")
  void resetPassword_success_whenUserExists() {
    // given
    UUID userId = UUID.randomUUID();
    UUID temporaryPasswordId = UUID.randomUUID();
    String email = "user@example.com";
    String rawTemporaryPassword = "Temp1234!";
    String encodedTemporaryPassword = "encoded-temporary-password";
    User user = user(userId, email);

    when(userRepository.findByEmailAndIsDeletedFalse(email)).thenReturn(Optional.of(user));
    when(temporaryPasswordGenerator.generate()).thenReturn(rawTemporaryPassword);
    when(passwordEncoder.encode(rawTemporaryPassword)).thenReturn(encodedTemporaryPassword);
    when(temporaryPasswordRepository.save(any(TemporaryPassword.class)))
        .thenAnswer(
            invocation -> {
              TemporaryPassword temporaryPassword = invocation.getArgument(0);
              ReflectionTestUtils.setField(temporaryPassword, "id", temporaryPasswordId);
              return temporaryPassword;
            });

    // when
    temporaryPasswordService.resetPassword(email);

    // then
    ArgumentCaptor<TemporaryPassword> temporaryPasswordCaptor =
        ArgumentCaptor.forClass(TemporaryPassword.class);
    ArgumentCaptor<TemporaryPasswordIssuedEvent> eventCaptor =
        ArgumentCaptor.forClass(TemporaryPasswordIssuedEvent.class);

    verify(userRepository).findByEmailAndIsDeletedFalse(email);
    verify(temporaryPasswordRepository).deleteByUserId(userId);
    verify(temporaryPasswordGenerator).generate();
    verify(passwordEncoder).encode(rawTemporaryPassword);
    verify(temporaryPasswordRepository).save(temporaryPasswordCaptor.capture());
    verify(eventPublisher).publishEvent(eventCaptor.capture());

    TemporaryPassword savedTemporaryPassword = temporaryPasswordCaptor.getValue();
    TemporaryPasswordIssuedEvent event = eventCaptor.getValue();

    assertAll(
        () -> assertEquals(user, savedTemporaryPassword.getUser()),
        () -> assertNotEquals(rawTemporaryPassword, savedTemporaryPassword.getPasswordHash()),
        () -> assertEquals(encodedTemporaryPassword, savedTemporaryPassword.getPasswordHash()),
        () -> assertEquals(FIXED_NOW.plusSeconds(180), savedTemporaryPassword.getExpiresAt()),
        () -> assertEquals(userId, event.userId()),
        () -> assertEquals(temporaryPasswordId, event.temporaryPasswordId()),
        () -> assertEquals(email, event.email()),
        () -> assertEquals(rawTemporaryPassword, event.temporaryPassword()));
  }

  @Test
  @DisplayName("존재하지 않는 이메일이면 사용자 없음 예외를 발생시키고 임시 비밀번호를 발급하지 않는다")
  void resetPassword_throwsUserNotFoundException_whenUserDoesNotExist() {
    // given
    String email = "unknown@example.com";

    when(userRepository.findByEmailAndIsDeletedFalse(email)).thenReturn(Optional.empty());

    // when
    UserException exception =
        assertThrows(UserException.class, () -> temporaryPasswordService.resetPassword(email));

    // then
    verify(userRepository).findByEmailAndIsDeletedFalse(email);
    verifyNoInteractions(
        temporaryPasswordRepository, passwordEncoder, temporaryPasswordGenerator, eventPublisher);
    assertEquals(UserErrorCode.USER_NOT_FOUND, exception.getErrorCode());
  }

  @Test
  @DisplayName("발급된 임시 비밀번호 보상 삭제는 사용자와 임시 비밀번호 식별자로 삭제한다")
  void deleteIssuedTemporaryPassword_deletesByTemporaryPasswordIdAndUserId() {
    // given
    UUID userId = UUID.randomUUID();
    UUID temporaryPasswordId = UUID.randomUUID();

    // when
    temporaryPasswordService.deleteIssuedTemporaryPassword(userId, temporaryPasswordId);

    // then
    verify(temporaryPasswordRepository).deleteByIdAndUserId(temporaryPasswordId, userId);
  }

  @Test
  @DisplayName("사용자 ID로 임시 비밀번호를 삭제한다")
  void deleteByUserId_deletesTemporaryPasswordByUserId() {
    // given
    UUID userId = UUID.randomUUID();

    // when
    temporaryPasswordService.deleteByUserId(userId);

    // then
    verify(temporaryPasswordRepository).deleteByUserId(userId);
  }

  private User user(UUID userId, String email) {
    User user = User.createUser("test-user", email, "encoded-password", null);
    ReflectionTestUtils.setField(user, "id", userId);
    return user;
  }
}
