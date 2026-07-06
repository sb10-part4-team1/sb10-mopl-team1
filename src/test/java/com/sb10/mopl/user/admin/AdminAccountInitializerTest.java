package com.sb10.mopl.user.admin;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sb10.mopl.auth.service.AuthSessionService;
import com.sb10.mopl.user.entity.User;
import com.sb10.mopl.user.entity.UserRole;
import com.sb10.mopl.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:admin_initializer_test;"
          + "MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
    })
@ActiveProfiles("test")
class AdminAccountInitializerTest {

  private static final String ADMIN_EMAIL = "admin@example.com";
  private static final String ADMIN_NAME = "mopl-admin";
  private static final String ADMIN_PASSWORD = "admin-password123";

  private final AuthSessionService authSessionService = mock(AuthSessionService.class);

  @Autowired private PasswordEncoder passwordEncoder;

  @Autowired private UserRepository userRepository;

  @Autowired private PlatformTransactionManager transactionManager;

  @BeforeEach
  void setUp() {
    userRepository.deleteAll();
    reset(authSessionService);
  }

  @Test
  @DisplayName("관리자 계정이 없으면 설정값으로 최초 관리자 계정을 생성한다")
  void run_success_whenAdminAccountDoesNotExist() {
    // when
    initializer(false).run(null);

    // then
    User admin = findAdminAccount();

    assertAll(
        () -> assertEquals(ADMIN_NAME, admin.getName()),
        () -> assertEquals(ADMIN_EMAIL, admin.getEmail()),
        () -> assertEquals(UserRole.ADMIN, admin.getRole()),
        () -> assertFalse(admin.isLocked()),
        () -> assertFalse(admin.isDeleted()));
  }

  @Test
  @DisplayName("초기화 로직을 중복 실행해도 관리자 계정은 중복 생성되지 않는다")
  void run_success_whenInitializerRunsRepeatedly() {
    // when
    initializer(false).run(null);
    initializer(false).run(null);

    // then
    List<User> admins =
        userRepository.findAll().stream()
            .filter(user -> ADMIN_EMAIL.equals(user.getEmail()))
            .toList();

    assertEquals(1, admins.size());
  }

  @Test
  @DisplayName("관리자 계정 생성 중 이메일 충돌이 발생하면 기존 관리자 계정 보정으로 복구한다")
  @SuppressWarnings("unchecked")
  void run_success_whenCreateConflictIsRecoveredByExistingAdminCalibration() {
    // given
    UserRepository conflictedUserRepository = mock(UserRepository.class);
    User existingAdmin =
        User.createAdmin(
            ADMIN_NAME, ADMIN_EMAIL, passwordEncoder.encode("raced-password123"), null);

    when(conflictedUserRepository.findByEmail(ADMIN_EMAIL))
        .thenReturn(Optional.empty(), Optional.of(existingAdmin));
    doThrow(new DataIntegrityViolationException("duplicate email"))
        .when(conflictedUserRepository)
        .saveAndFlush(any());

    // when
    initializer(false, conflictedUserRepository).run(null);

    // then
    verify(conflictedUserRepository, times(2)).findByEmail(ADMIN_EMAIL);
    verify(conflictedUserRepository).saveAndFlush(any());
  }

  @Test
  @DisplayName("기존 관리자 계정이 잠겨 있으면 잠금 해제 상태로 보정한다")
  void run_success_whenExistingAdminIsLocked() {
    // given
    User admin =
        User.createAdmin(ADMIN_NAME, ADMIN_EMAIL, passwordEncoder.encode("old-password123"), null);
    admin.changeLocked(true);
    final UUID adminId = userRepository.saveAndFlush(admin).getId();

    // when
    initializer(false).run(null);

    // then
    User calibratedAdmin = findAdminAccount();

    assertAll(
        () -> assertEquals(UserRole.ADMIN, calibratedAdmin.getRole()),
        () -> assertFalse(calibratedAdmin.isLocked()));
    verify(authSessionService).invalidateAllByUserId(adminId);
  }

  @Test
  @DisplayName("비밀번호 덮어쓰기 정책이 꺼져 있으면 기존 관리자 비밀번호를 유지한다")
  void run_success_whenPasswordOverwriteIsDisabled() {
    // given
    final String oldPasswordHash = passwordEncoder.encode("old-password123");
    User admin = User.createAdmin(ADMIN_NAME, ADMIN_EMAIL, oldPasswordHash, null);
    userRepository.saveAndFlush(admin);

    // when
    initializer(false).run(null);

    // then
    User unchangedAdmin = findAdminAccount();

    assertEquals(oldPasswordHash, unchangedAdmin.passwordHash());
    verify(authSessionService, never()).invalidateAllByUserId(any());
  }

  @Test
  @DisplayName("비밀번호 덮어쓰기 정책이 켜져 있으면 설정 비밀번호로 교체한다")
  void run_success_whenPasswordOverwriteIsEnabled() {
    // given
    final String oldPasswordHash = passwordEncoder.encode("old-password123");
    User admin = User.createAdmin(ADMIN_NAME, ADMIN_EMAIL, oldPasswordHash, null);
    final UUID adminId = userRepository.saveAndFlush(admin).getId();

    // when
    initializer(true).run(null);

    // then
    User changedAdmin = findAdminAccount();
    String changedPasswordHash = changedAdmin.passwordHash();

    assertAll(
        () -> assertNotEquals(oldPasswordHash, changedPasswordHash),
        () -> assertTrue(passwordEncoder.matches(ADMIN_PASSWORD, changedPasswordHash)));
    verify(authSessionService).invalidateAllByUserId(adminId);
  }

  @Test
  @DisplayName("설정된 관리자 이메일을 일반 사용자가 사용 중이면 초기화에 실패한다")
  void run_fail_whenConfiguredEmailBelongsToUser() {
    // given
    User user =
        User.createUser(
            "existing-user", ADMIN_EMAIL, passwordEncoder.encode("user-password123"), null);
    userRepository.saveAndFlush(user);

    // when
    IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> initializer(false).run(null));

    User existingUser = userRepository.findByEmail(ADMIN_EMAIL).orElseThrow();

    // then
    assertAll(
        () ->
            assertEquals(
                "Configured admin account email belongs to a non-admin user.",
                exception.getMessage()),
        () -> assertEquals(UserRole.USER, existingUser.getRole()),
        () -> assertEquals(1, userRepository.findAll().size()));
  }

  @Test
  @DisplayName("설정된 관리자 이메일이 삭제된 계정에 있으면 초기화에 실패한다")
  void run_fail_whenConfiguredEmailBelongsToDeletedAccount() {
    // given
    User deletedAdmin =
        User.createAdmin(ADMIN_NAME, ADMIN_EMAIL, passwordEncoder.encode("old-password123"), null);
    deletedAdmin.softDelete();
    userRepository.saveAndFlush(deletedAdmin);

    // when
    IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> initializer(false).run(null));

    User existingUser = userRepository.findByEmail(ADMIN_EMAIL).orElseThrow();

    // then
    assertAll(
        () -> assertEquals("Configured admin account is deleted.", exception.getMessage()),
        () -> assertTrue(existingUser.isDeleted()),
        () -> assertEquals(1, userRepository.findAll().size()));
  }

  @Test
  @DisplayName("관리자 비밀번호는 원문이 아닌 암호화된 값으로 저장한다")
  void run_success_whenAdminPasswordIsEncoded() {
    // when
    initializer(false).run(null);

    // then
    User admin = findAdminAccount();
    String savedPassword = admin.passwordHash();

    assertAll(
        () -> assertNotEquals(ADMIN_PASSWORD, savedPassword),
        () -> assertTrue(passwordEncoder.matches(ADMIN_PASSWORD, savedPassword)));
  }

  private AdminAccountInitializer initializer(boolean overwritePassword) {
    return initializer(overwritePassword, userRepository);
  }

  private AdminAccountInitializer initializer(
      boolean overwritePassword, UserRepository targetUserRepository) {
    AdminAccountProperties properties =
        new AdminAccountProperties(
            new AdminAccountProperties.Initializer(true, overwritePassword),
            new AdminAccountProperties.Account(ADMIN_EMAIL, ADMIN_NAME, ADMIN_PASSWORD));
    return new AdminAccountInitializer(
        properties,
        targetUserRepository,
        passwordEncoder,
        authSessionService,
        new TransactionTemplate(transactionManager));
  }

  private User findAdminAccount() {
    return userRepository.findByEmail(ADMIN_EMAIL).orElseThrow();
  }
}
