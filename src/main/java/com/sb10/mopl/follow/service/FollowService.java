package com.sb10.mopl.follow.service;

import com.sb10.mopl.follow.dto.FollowDto;
import com.sb10.mopl.follow.dto.FollowRequest;
import java.util.UUID;

public interface FollowService {

  FollowDto follow(UUID followerId, FollowRequest request);

  void unfollow(UUID userId, UUID followId);

  // 현재 로그인한 사용자가 특정 유저를 팔로우 중인지 조회
  FollowDto findFollowedByMe(UUID followerId, UUID followeeId);

  // 특정 유저를 팔로우하는 사람 수 조회
  long countFollowers(UUID followeeId);
}
