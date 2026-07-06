package com.sb10.mopl.follow.repository;

import com.sb10.mopl.follow.entity.Follow;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FollowRepository extends JpaRepository<Follow, UUID> {
  boolean existsByFollowerIdAndFolloweeId(UUID followerId, UUID followeeId);
}
