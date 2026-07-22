package com.sb10.mopl.follow.repository;

import com.sb10.mopl.follow.entity.Follow;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FollowRepository extends JpaRepository<Follow, UUID> {

  boolean existsByFollowerIdAndFolloweeId(UUID followerId, UUID followeeId);

  // 특정 유저를 내가 팔로우 중인지 조회
  Optional<Follow> findByFollowerIdAndFolloweeId(UUID followerId, UUID followeeId);

  // 특정 유저의 팔로워 수 조회
  long countByFolloweeId(UUID followeeId);

  // 특정 유저를 팔로우 중인 유저 id 목록 (팔로우한 사용자의 활동 알림용)
  @Query("SELECT f.followerId FROM Follow f WHERE f.followeeId = :followeeId")
  List<UUID> findFollowerIdsByFolloweeId(@Param("followeeId") UUID followeeId);
}
