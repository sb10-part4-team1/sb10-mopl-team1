package com.sb10.mopl.follow.repository;

import java.util.List;
import java.util.UUID;

public interface FollowRepositoryCustom {

  List<UUID> findFollowerIdsByFolloweeId(java.util.UUID followeeId);
}
