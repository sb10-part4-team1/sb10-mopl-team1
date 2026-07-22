package com.sb10.mopl.follow.repository;

import java.util.List;
import java.util.UUID;

public interface FollowRepositoryCustom {

  // 특정 사용자를 팔로우하는 사용자 ID 목록을 커서 기반으로 조회
  List<UUID> findFollowerIdsByFolloweeId(UUID followeeId, UUID idAfter, int limit);
}
