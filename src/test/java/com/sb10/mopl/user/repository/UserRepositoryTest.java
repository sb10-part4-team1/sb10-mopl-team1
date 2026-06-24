package com.sb10.mopl.user.repository;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sb10.mopl.config.JpaAuditingConfig;
import com.sb10.mopl.user.entity.User;
import com.sb10.mopl.user.entity.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest(
    properties = {"spring.sql.init.mode=never", "spring.jpa.hibernate.ddl-auto=create-drop"})
@Import(JpaAuditingConfig.class)
class UserRepositoryTest {

  @Autowired private UserRepository userRepository;

  @Test
  @DisplayName("사용자를 저장하면 기본 권한과 잠금 해제 상태가 유지된다")
  void save_success_whenUserIsValid() {
    // given
    User user = User.createUser("test-user", "user@example.com", "encoded-password", null);

    // when
    User savedUser = userRepository.saveAndFlush(user);

    // then
    assertAll(
        () -> assertNotNull(savedUser.getId()),
        () -> assertNotNull(savedUser.getCreatedAt()),
        () -> assertEquals("test-user", savedUser.getName()),
        () -> assertEquals("user@example.com", savedUser.getEmail()),
        () -> assertEquals(UserRole.USER, savedUser.getRole()),
        () -> assertFalse(savedUser.isLocked()));
  }

  @Test
  @DisplayName("이메일로 사용자 존재 여부를 조회한다")
  void existsByEmail_success_whenUserExists() {
    // given
    userRepository.saveAndFlush(
        User.createUser("test-user", "user@example.com", "encoded-password", null));

    // when & then
    assertTrue(userRepository.existsByEmail("user@example.com"));
    assertFalse(userRepository.existsByEmail("missing@example.com"));
  }

  @Test
  @DisplayName("중복 이메일 저장 시 예외가 발생한다")
  void save_fail_whenEmailAlreadyExists() {
    // given
    userRepository.saveAndFlush(
        User.createUser("first-user", "user@example.com", "encoded-password", null));
    User duplicatedUser =
        User.createUser("second-user", "user@example.com", "encoded-password", null);

    // when & then
    assertThrows(
        DataIntegrityViolationException.class, () -> userRepository.saveAndFlush(duplicatedUser));
  }
}
