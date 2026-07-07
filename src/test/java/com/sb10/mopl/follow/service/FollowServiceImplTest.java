package com.sb10.mopl.follow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.sb10.mopl.follow.dto.FollowDto;
import com.sb10.mopl.follow.dto.FollowRequest;
import com.sb10.mopl.follow.entity.Follow;
import com.sb10.mopl.follow.exception.FollowErrorCode;
import com.sb10.mopl.follow.exception.FollowException;
import com.sb10.mopl.follow.mapper.FollowMapper;
import com.sb10.mopl.follow.repository.FollowRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FollowServiceImplTest {

  @Mock private FollowRepository followRepository;

  @Mock private FollowMapper followMapper;

  @InjectMocks private FollowServiceImpl followService;

  private UUID followId;
  private UUID followerId;
  private UUID followeeId;
  private UUID otherUserId;

  private Follow follow;
  private FollowRequest request;
  private FollowDto followDto;

  @BeforeEach
  void setUp() {
    followId = UUID.randomUUID();
    followerId = UUID.randomUUID();
    followeeId = UUID.randomUUID();
    otherUserId = UUID.randomUUID();

    follow = createFollow(followId, followerId, followeeId);
    request = new FollowRequest(followeeId);
    followDto = new FollowDto(followId, followerId, followeeId);
  }

  @Test
  @DisplayName("팔로우 - 생성 성공")
  void follow_success() {
    // given
    given(followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId))
        .willReturn(false);
    given(followMapper.toEntity(followerId, request)).willReturn(follow);
    given(followRepository.save(follow)).willReturn(follow);
    given(followMapper.toDto(follow)).willReturn(followDto);

    // when
    FollowDto result = followService.follow(followerId, request);

    // then
    assertThat(result).isEqualTo(followDto);
    verify(followRepository).save(follow);
  }

  @Test
  @DisplayName("팔로우 - 생성 실패 - 이미 팔로우한 관계")
  void follow_fail_alreadyExists() {
    // given
    given(followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId))
        .willReturn(true);

    // when & then
    assertThatThrownBy(() -> followService.follow(followerId, request))
        .isInstanceOf(FollowException.class)
        .extracting("errorCode")
        .isEqualTo(FollowErrorCode.FOLLOW_ALREADY_EXISTS);

    verify(followRepository, never()).save(any());
  }

  @Test
  @DisplayName("언팔로우 - 삭제 성공")
  void unfollow_success() {
    // given
    given(followRepository.findById(followId)).willReturn(Optional.of(follow));

    // when
    followService.unfollow(followerId, followId);

    // then
    verify(followRepository).delete(follow);
  }

  @Test
  @DisplayName("언팔로우 - 삭제 실패 - 팔로우가 존재하지 않음")
  void unfollow_fail_followNotFound() {
    // given
    given(followRepository.findById(followId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> followService.unfollow(followerId, followId))
        .isInstanceOf(FollowException.class)
        .extracting("errorCode")
        .isEqualTo(FollowErrorCode.FOLLOW_NOT_FOUND);

    verify(followRepository, never()).delete(any());
  }

  @Test
  @DisplayName("언팔로우 - 삭제 실패 - 팔로우 요청자가 아님")
  void unfollow_fail_unauthorizedOwner() {
    // given
    given(followRepository.findById(followId)).willReturn(Optional.of(follow));

    // when & then
    assertThatThrownBy(() -> followService.unfollow(otherUserId, followId))
        .isInstanceOf(FollowException.class)
        .extracting("errorCode")
        .isEqualTo(FollowErrorCode.UNAUTHORIZED_FOLLOW_ACCESS);

    verify(followRepository, never()).delete(any());
  }

  @Test
  @DisplayName("내가 특정 유저를 팔로우 중인지 조회 - 성공")
  void findFollowedByMe_success() {
    // given
    given(followRepository.findByFollowerIdAndFolloweeId(followerId, followeeId))
        .willReturn(Optional.of(follow));
    given(followMapper.toDto(follow)).willReturn(followDto);

    // when
    FollowDto result = followService.findFollowedByMe(followerId, followeeId);

    // then
    assertThat(result).isEqualTo(followDto);
  }

  @Test
  @DisplayName("내가 특정 유저를 팔로우 중인지 조회 - 실패 - 팔로우 관계가 없음")
  void findFollowedByMe_fail_followNotFound() {
    // given
    given(followRepository.findByFollowerIdAndFolloweeId(followerId, followeeId))
        .willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> followService.findFollowedByMe(followerId, followeeId))
        .isInstanceOf(FollowException.class)
        .extracting("errorCode")
        .isEqualTo(FollowErrorCode.FOLLOW_NOT_FOUND);
  }

  @Test
  @DisplayName("특정 유저의 팔로워 수 조회 - 성공")
  void countFollowers_success() {
    // given
    given(followRepository.countByFolloweeId(followeeId)).willReturn(2L);

    // when
    long result = followService.countFollowers(followeeId);

    // then
    assertThat(result).isEqualTo(2L);
  }

  // 테스트용 Follow 생성 후 id 주입
  private Follow createFollow(UUID followId, UUID followerId, UUID followeeId) {
    Follow follow = new Follow(followerId, followeeId);
    ReflectionTestUtils.setField(follow, "id", followId);
    return follow;
  }
}
