package com.sb10.mopl.follow.service;

import com.sb10.mopl.follow.dto.FollowDto;
import com.sb10.mopl.follow.dto.FollowRequest;
import com.sb10.mopl.follow.entity.Follow;
import com.sb10.mopl.follow.exception.FollowErrorCode;
import com.sb10.mopl.follow.exception.FollowException;
import com.sb10.mopl.follow.mapper.FollowMapper;
import com.sb10.mopl.follow.repository.FollowRepository;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FollowServiceImpl implements FollowService {

  private final FollowRepository followRepository;
  private final FollowMapper followMapper;

  @Override
  @Transactional
  public FollowDto follow(UUID followerId, FollowRequest request) {
    validateFollowNotExists(followerId, request.followeeId());

    Follow follow = followMapper.toEntity(followerId, request);
    Follow savedFollow = followRepository.save(follow);

    return followMapper.toDto(savedFollow);
  }

  @Override
  @Transactional
  public void unfollow(UUID userId, UUID followId) {
    Follow follow = getFollowOwnedBy(followId, userId);

    followRepository.delete(follow);
  }

  // 이미 팔로우한 관계인지 검증
  private void validateFollowNotExists(UUID followerId, UUID followeeId) {
    if (followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId)) {
      throw new FollowException(
          FollowErrorCode.FOLLOW_ALREADY_EXISTS,
          Map.of("followerId", followerId, "followeeId", followeeId));
    }
  }

  // 팔로우 존재 여부와 요청자 권한을 함께 검증
  private Follow getFollowOwnedBy(UUID followId, UUID userId) {
    Follow follow =
        followRepository
            .findById(followId)
            .orElseThrow(
                () ->
                    new FollowException(
                        FollowErrorCode.FOLLOW_NOT_FOUND, Map.of("followId", followId)));

    validateFollowOwner(follow, userId);

    return follow;
  }

  // 팔로우 요청자 권한 검증
  private void validateFollowOwner(Follow follow, UUID userId) {
    if (!follow.getFollowerId().equals(userId)) {
      throw new FollowException(
          FollowErrorCode.UNAUTHORIZED_FOLLOW_ACCESS,
          Map.of("followId", follow.getId(), "userId", userId));
    }
  }
}
