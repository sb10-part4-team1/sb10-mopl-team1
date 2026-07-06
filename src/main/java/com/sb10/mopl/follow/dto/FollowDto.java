package com.sb10.mopl.follow.dto;

import java.util.UUID;

public record FollowDto(
  UUID id,
  UUID followerId,
  UUID followeeId
) {}
