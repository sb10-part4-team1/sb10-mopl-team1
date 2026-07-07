package com.sb10.mopl.user.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sb10.mopl.common.pagination.SortDirection;
import com.sb10.mopl.config.JpaAuditingConfig;
import com.sb10.mopl.config.QuerydslConfig;
import com.sb10.mopl.user.dto.request.UserSearchRequest;
import com.sb10.mopl.user.entity.User;
import com.sb10.mopl.user.entity.UserRole;
import com.sb10.mopl.user.exception.UserErrorCode;
import com.sb10.mopl.user.exception.UserException;
import jakarta.persistence.EntityManager;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest(
    properties = {"spring.sql.init.mode=never", "spring.jpa.hibernate.ddl-auto=create-drop"})
@Import({JpaAuditingConfig.class, QuerydslConfig.class})
class UserRepositoryTest {

  @Autowired private UserRepository userRepository;

  @Autowired private EntityManager entityManager;

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

  @Test
  @DisplayName("사용자 목록 조회 - emailLike, roleEqual, isLocked 필터를 함께 적용한다")
  void findAllByCondition_success_whenFiltersAreProvided() {
    // given
    saveUser("admin", "admin.alpha@example.com", UserRole.ADMIN, false);
    final User matched = saveUser("locked-user", "locked.alpha@example.com", UserRole.USER, true);
    saveUser("active-user", "active.alpha@example.com", UserRole.USER, false);
    saveUser("other-user", "other@example.com", UserRole.USER, true);

    entityManager.flush();
    entityManager.clear();

    UserSearchRequest request =
        new UserSearchRequest(
            "ALPHA",
            UserRole.USER,
            true,
            null,
            null,
            10,
            SortDirection.ASCENDING,
            UserSearchRequest.SortBy.email);

    // when
    List<User> users = userRepository.findAllByCondition(request);
    long totalCount = userRepository.countByCondition(request);

    // then
    assertThat(users).extracting(User::getId).containsExactly(matched.getId());
    assertThat(totalCount).isEqualTo(1L);
  }

  @Test
  @DisplayName("사용자 목록 조회 - name, email, createdAt, isLocked, role 정렬을 지원한다")
  void findAllByCondition_success_whenSortingBySupportedFields() throws InterruptedException {
    // given
    final User charlie = saveUser("charlie", "charlie@example.com", UserRole.USER, false);
    Thread.sleep(10);
    final User alice = saveUser("alice", "alice@example.com", UserRole.ADMIN, true);
    Thread.sleep(10);
    final User bob = saveUser("bob", "bob@example.com", UserRole.USER, false);

    entityManager.flush();
    entityManager.clear();

    // when & then
    assertThat(findUsersSortedBy(UserSearchRequest.SortBy.name, SortDirection.ASCENDING))
        .extracting(User::getName)
        .containsExactly("alice", "bob", "charlie");

    assertThat(findUsersSortedBy(UserSearchRequest.SortBy.email, SortDirection.DESCENDING))
        .extracting(User::getEmail)
        .containsExactly("charlie@example.com", "bob@example.com", "alice@example.com");

    assertThat(findUsersSortedBy(UserSearchRequest.SortBy.createdAt, SortDirection.ASCENDING))
        .extracting(User::getId)
        .containsExactly(charlie.getId(), alice.getId(), bob.getId());

    assertThat(findUsersSortedBy(UserSearchRequest.SortBy.isLocked, SortDirection.ASCENDING))
        .extracting(User::isLocked)
        .containsExactly(false, false, true);

    assertThat(findUsersSortedBy(UserSearchRequest.SortBy.role, SortDirection.ASCENDING))
        .extracting(User::getRole)
        .containsExactly(UserRole.ADMIN, UserRole.USER, UserRole.USER);
  }

  @Test
  @DisplayName("사용자 목록 조회 - 동일한 정렬 값에서는 idAfter로 중복 없이 다음 페이지를 조회한다")
  void findAllByCondition_success_whenCursorUsesIdAfterForTiedSortValues() {
    // given
    User first = saveUser("same-name", "first@example.com", UserRole.USER, false);
    User second = saveUser("same-name", "second@example.com", UserRole.USER, false);
    User third = saveUser("same-name", "third@example.com", UserRole.USER, false);
    final Set<UUID> savedIds = Set.of(first.getId(), second.getId(), third.getId());

    entityManager.flush();
    entityManager.clear();

    UserSearchRequest firstPageRequest =
        new UserSearchRequest(
            null,
            null,
            null,
            null,
            null,
            2,
            SortDirection.ASCENDING,
            UserSearchRequest.SortBy.name);
    List<User> firstPageWithLookAhead = userRepository.findAllByCondition(firstPageRequest);
    List<User> firstPage = firstPageWithLookAhead.subList(0, 2);
    User lastUser = firstPage.get(firstPage.size() - 1);

    UserSearchRequest secondPageRequest =
        new UserSearchRequest(
            null,
            null,
            null,
            lastUser.getName(),
            lastUser.getId(),
            2,
            SortDirection.ASCENDING,
            UserSearchRequest.SortBy.name);

    // when
    List<User> secondPage = userRepository.findAllByCondition(secondPageRequest);

    // then
    Set<UUID> pagedIds = new HashSet<>();
    firstPage.forEach(user -> pagedIds.add(user.getId()));
    secondPage.forEach(user -> pagedIds.add(user.getId()));

    assertThat(firstPageWithLookAhead).hasSize(3);
    assertThat(secondPage).hasSize(1);
    assertThat(pagedIds).isEqualTo(savedIds);
  }

  @Test
  @DisplayName("사용자 목록 조회 - cursor 형식이 정렬 기준과 맞지 않으면 예외가 발생한다")
  void findAllByCondition_fail_whenCursorIsMalformed() {
    // given
    User user = saveUser("test-user", "user@example.com", UserRole.USER, false);
    UserSearchRequest request =
        new UserSearchRequest(
            null,
            null,
            null,
            "not-an-instant",
            user.getId(),
            10,
            SortDirection.ASCENDING,
            UserSearchRequest.SortBy.createdAt);

    // when & then
    assertThatThrownBy(() -> userRepository.findAllByCondition(request))
        .isInstanceOf(UserException.class)
        .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.INVALID_USER_VALUE);
  }

  private List<User> findUsersSortedBy(
      UserSearchRequest.SortBy sortBy, SortDirection sortDirection) {
    UserSearchRequest request =
        new UserSearchRequest(null, null, null, null, null, 10, sortDirection, sortBy);
    return userRepository.findAllByCondition(request);
  }

  private User saveUser(String name, String email, UserRole role, boolean locked) {
    User user =
        role == UserRole.ADMIN
            ? User.createAdmin(name, email, "encoded-password", null)
            : User.createUser(name, email, "encoded-password", null);
    user.changeLocked(locked);
    return userRepository.saveAndFlush(user);
  }
}
