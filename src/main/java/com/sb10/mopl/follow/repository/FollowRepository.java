package com.sb10.mopl.follow.repository;

import com.sb10.mopl.follow.entity.Follow;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FollowRepository extends JpaRepository<Follow, UUID> {
  // TODO: #47 팔로우/언팔로우 기능 구현 시 필요한 조회 메서드 추가
}
