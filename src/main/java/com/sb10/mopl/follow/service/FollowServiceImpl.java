package com.sb10.mopl.follow.service;

import com.sb10.mopl.follow.dto.FollowDto;
import com.sb10.mopl.follow.dto.FollowRequest;
import com.sb10.mopl.follow.entity.Follow;
import com.sb10.mopl.follow.event.FollowCreatedEvent;
import com.sb10.mopl.follow.exception.FollowErrorCode;
import com.sb10.mopl.follow.exception.FollowException;
import com.sb10.mopl.follow.mapper.FollowMapper;
import com.sb10.mopl.follow.repository.FollowRepository;
import com.sb10.mopl.user.exception.UserErrorCode;
import com.sb10.mopl.user.exception.UserException;
import com.sb10.mopl.user.repository.UserRepository;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FollowServiceImpl implements FollowService {

  private final FollowRepository followRepository;
  private final FollowMapper followMapper;
  private final UserRepository userRepository;
  private final ApplicationEventPublisher eventPublisher;

  @Override
  @Transactional
  public FollowDto follow(UUID followerId, FollowRequest request) {
    UUID followeeId = request.followeeId();

    validateFolloweeExists(request.followeeId());
    validateFollowNotExists(followerId, request.followeeId());

    Follow follow = followMapper.toEntity(followerId, request);
    Follow savedFollow = followRepository.save(follow);

    eventPublisher.publishEvent(new FollowCreatedEvent(followerId, followeeId));

    return followMapper.toDto(savedFollow);
  }

  @Override
  @Transactional
  public void unfollow(UUID userId, UUID followId) {
    Follow follow = getFollowOwnedBy(followId, userId);

    followRepository.delete(follow);
  }

  // 현재 로그인한 사용자가 특정 유저를 팔로우 중인지 조회
  @Override
  public FollowDto findFollowedByMe(UUID followerId, UUID followeeId) {
    Follow follow = getFollowByFollowerAndFollowee(followerId, followeeId);

    return followMapper.toDto(follow);
  }

  // 특정 유저를 팔로우하는 사람 수 조회
  @Override
  public long countFollowers(UUID followeeId) {
    return followRepository.countByFolloweeId(followeeId);
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

  // 팔로우 대상 유저 존재 여부 검증
  private void validateFolloweeExists(UUID followeeId) {
    userRepository
        .findByIdAndIsDeletedFalse(followeeId)
        .orElseThrow(
            () ->
                new UserException(UserErrorCode.USER_NOT_FOUND, Map.of("followeeId", followeeId)));
  }

  // 팔로워와 팔로우 대상자로 팔로우 관계 조회
  private Follow getFollowByFollowerAndFollowee(UUID followerId, UUID followeeId) {
    return followRepository
        .findByFollowerIdAndFolloweeId(followerId, followeeId)
        .orElseThrow(
            () ->
                new FollowException(
                    FollowErrorCode.FOLLOW_NOT_FOUND,
                    Map.of("followerId", followerId, "followeeId", followeeId)));
  }
}
