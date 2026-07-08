package com.sb10.mopl.follow.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sb10.mopl.follow.exception.FollowErrorCode;
import com.sb10.mopl.follow.exception.FollowException;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FollowTest {

  @Test
  @DisplayName("팔로우 - 생성 성공")
  void create_success() {
    // given
    UUID followerId = UUID.randomUUID();
    UUID followeeId = UUID.randomUUID();

    // when
    Follow follow = new Follow(followerId, followeeId);

    // then
    assertThat(follow.getFollowerId()).isEqualTo(followerId);
    assertThat(follow.getFolloweeId()).isEqualTo(followeeId);
  }

  @Test
  @DisplayName("팔로우 - 생성 실패 - 팔로우 요청자가 없음")
  void create_fail_followerIdNull() {
    // when & then
    assertThatThrownBy(() -> new Follow(null, UUID.randomUUID()))
        .isInstanceOf(FollowException.class)
        .extracting("errorCode")
        .isEqualTo(FollowErrorCode.INVALID_FOLLOW_VALUE);
  }

  @Test
  @DisplayName("팔로우 - 생성 실패 - 팔로우 대상자가 없음")
  void create_fail_followeeIdNull() {
    // when & then
    assertThatThrownBy(() -> new Follow(UUID.randomUUID(), null))
        .isInstanceOf(FollowException.class)
        .extracting("errorCode")
        .isEqualTo(FollowErrorCode.INVALID_FOLLOW_VALUE);
  }

  @Test
  @DisplayName("팔로우 - 생성 실패 - 자기 자신을 팔로우함")
  void create_fail_selfFollow() {
    // given
    UUID userId = UUID.randomUUID();

    // when & then
    assertThatThrownBy(() -> new Follow(userId, userId))
        .isInstanceOf(FollowException.class)
        .extracting("errorCode")
        .isEqualTo(FollowErrorCode.INVALID_FOLLOW_VALUE);
  }
}
