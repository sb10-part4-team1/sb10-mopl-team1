package com.sb10.mopl.auth.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.sb10.mopl.user.entity.User;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class SocialAccountTest {

  private User user;

  @BeforeEach
  void setUp() {
    user = createUser(UUID.randomUUID());
  }

  @Test
  @DisplayName("소셜 계정 - 생성 성공")
  void create_success() {
    // when
    SocialAccount socialAccount =
        SocialAccount.create(user, SocialProvider.GOOGLE, "provider-user-id");

    // then
    assertThat(socialAccount.getUser()).isEqualTo(user);
    assertThat(socialAccount.getProvider()).isEqualTo(SocialProvider.GOOGLE);
    assertThat(socialAccount.getProviderUserId()).isEqualTo("provider-user-id");
  }

  private User createUser(UUID userId) {
    User user = User.createUser("테스트유저", "test@example.com", "password", null);
    ReflectionTestUtils.setField(user, "id", userId);
    return user;
  }
}
