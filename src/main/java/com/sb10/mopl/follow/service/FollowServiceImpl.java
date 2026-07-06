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
    UUID followeeId = request.followeeId();

    if (followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId)) {
      throw new FollowException(
          FollowErrorCode.FOLLOW_ALREADY_EXISTS,
          Map.of(
              "followerId", followerId,
              "followeeId", followeeId));
    }

    Follow follow = followMapper.toEntity(followerId, request);
    Follow savedFollow = followRepository.save(follow);

    return followMapper.toDto(savedFollow);
  }

  @Override
  @Transactional
  public void unfollow(UUID userId, UUID followId) {
    Follow follow =
        followRepository
            .findById(followId)
            .orElseThrow(
                () ->
                    new FollowException(
                        FollowErrorCode.FOLLOW_NOT_FOUND, Map.of("followId", followId)));

    if (!follow.getFollowerId().equals(userId)) {
      throw new FollowException(
          FollowErrorCode.UNAUTHORIZED_FOLLOW_ACCESS,
          Map.of(
              "userId", userId,
              "followId", followId,
              "followerId", follow.getFollowerId()));
    }

    followRepository.delete(follow);
  }
}
