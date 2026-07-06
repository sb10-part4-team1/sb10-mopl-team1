package com.sb10.mopl.follow.service;

import com.sb10.mopl.follow.dto.FollowDto;
import com.sb10.mopl.follow.dto.FollowRequest;
import java.util.UUID;

public interface FollowService {

  FollowDto follow(UUID followerId, FollowRequest request);

  void unfollow(UUID userId, UUID followId);
}
