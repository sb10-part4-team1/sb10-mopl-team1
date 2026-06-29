package com.sb10.mopl.auth.security.user;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.sb10.mopl.user.entity.User;
import com.sb10.mopl.user.entity.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MoplUserDetailsTest {

  @Test
  @DisplayName("잠긴 사용자의 인증 상세 정보는 계정 잠금 상태를 반영한다")
  void userDetails_containsLockedUserState() {
    User user = User.createAdmin("locked-admin", "admin@example.com", "encoded-password", null);
    user.changeLocked(true);

    MoplUserDetails userDetails = new MoplUserDetails(user);

    assertAll(
        () -> assertEquals(UserRole.ADMIN, userDetails.getRole()),
        () -> assertEquals("admin@example.com", userDetails.getUsername()),
        () -> assertFalse(userDetails.isAccountNonLocked()));
  }
}
