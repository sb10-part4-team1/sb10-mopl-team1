package com.sb10.mopl.playlistsubscription.repository;

import com.sb10.mopl.playlistsubscription.entity.PlaylistSubscription;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaylistSubscriptionRepository extends JpaRepository<PlaylistSubscription, UUID> {
  // 사용자가 특정 플레이리스트를 이미 구독 중인지 확인
  boolean existsBySubscriberIdAndPlaylistId(UUID subscriberId, UUID playlistId);

  // 사용자와 플레이리스트 기준으로 구독 정보를 조회
  Optional<PlaylistSubscription> findBySubscriberIdAndPlaylistId(
      UUID subscriberId, UUID playlistId);

  // 특정 플레이리스트의 구독자 수 조회
  long countByPlaylistId(UUID playlistId);
}
