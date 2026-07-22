package com.sb10.mopl.follow.repository;

import com.sb10.mopl.follow.entity.Follow;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FollowRepository extends JpaRepository<Follow, UUID> {
  boolean existsByFollowerIdAndFolloweeId(UUID followerId, UUID followeeId);

  // 특정 유저를 내가 팔로우 중인지 조회
  Optional<Follow> findByFollowerIdAndFolloweeId(UUID followerId, UUID followeeId);

  // 특정 유저의 팔로워 수 조회
  long countByFolloweeId(UUID followeeId);

  // 특정 사용자를 팔로우하는 사용자 ID 목록 조회
  List<UUID> findFollowerIdsByFolloweeId(UUID followeeId);
}
