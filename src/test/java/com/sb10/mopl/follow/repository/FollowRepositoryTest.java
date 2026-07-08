package com.sb10.mopl.follow.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sb10.mopl.config.JpaAuditingConfig;
import com.sb10.mopl.config.QuerydslConfig;
import com.sb10.mopl.follow.entity.Follow;
import java.util.Optional;
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
class FollowRepositoryTest {

  @Autowired private FollowRepository followRepository;

  @Test
  @DisplayName("팔로우를 저장하면 id와 생성 시간이 저장된다")
  void save_success_whenFollowIsValid() {
    // given
    Follow follow = new Follow(UUID.randomUUID(), UUID.randomUUID());

    // when
    Follow savedFollow = followRepository.saveAndFlush(follow);

    // then
    assertThat(savedFollow.getId()).isNotNull();
    assertThat(savedFollow.getCreatedAt()).isNotNull();
    assertThat(savedFollow.getFollowerId()).isEqualTo(follow.getFollowerId());
    assertThat(savedFollow.getFolloweeId()).isEqualTo(follow.getFolloweeId());
  }

  @Test
  @DisplayName("팔로워와 팔로우 대상자로 팔로우 존재 여부를 조회한다")
  void existsByFollowerIdAndFolloweeId_success() {
    // given
    UUID followerId = UUID.randomUUID();
    UUID followeeId = UUID.randomUUID();
    followRepository.saveAndFlush(new Follow(followerId, followeeId));

    // when & then
    assertThat(followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId)).isTrue();
    assertThat(followRepository.existsByFollowerIdAndFolloweeId(UUID.randomUUID(), followeeId))
        .isFalse();
  }

  @Test
  @DisplayName("팔로워와 팔로우 대상자로 팔로우를 조회한다")
  void findByFollowerIdAndFolloweeId_success() {
    // given
    UUID followerId = UUID.randomUUID();
    UUID followeeId = UUID.randomUUID();
    Follow savedFollow = followRepository.saveAndFlush(new Follow(followerId, followeeId));

    // when
    Optional<Follow> result =
        followRepository.findByFollowerIdAndFolloweeId(followerId, followeeId);

    // then
    assertThat(result).isPresent();
    assertThat(result.get().getId()).isEqualTo(savedFollow.getId());
  }

  @Test
  @DisplayName("특정 유저의 팔로워 수를 조회한다")
  void countByFolloweeId_success() {
    // given
    UUID followeeId = UUID.randomUUID();
    followRepository.save(new Follow(UUID.randomUUID(), followeeId));
    followRepository.save(new Follow(UUID.randomUUID(), followeeId));
    followRepository.save(new Follow(UUID.randomUUID(), UUID.randomUUID()));
    followRepository.flush();

    // when
    long count = followRepository.countByFolloweeId(followeeId);

    // then
    assertThat(count).isEqualTo(2L);
  }

  @Test
  @DisplayName("동일한 팔로워와 팔로우 대상자를 중복 저장하면 예외가 발생한다")
  void save_fail_whenFollowAlreadyExists() {
    // given
    UUID followerId = UUID.randomUUID();
    UUID followeeId = UUID.randomUUID();
    followRepository.saveAndFlush(new Follow(followerId, followeeId));

    // when & then
    assertThrows(
        DataIntegrityViolationException.class,
        () -> followRepository.saveAndFlush(new Follow(followerId, followeeId)));
  }
}
