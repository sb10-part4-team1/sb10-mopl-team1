package com.sb10.mopl.playlistsubscription.repository;

import com.sb10.mopl.playlistsubscription.entity.PlaylistSubscription;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlaylistSubscriptionRepository extends JpaRepository<PlaylistSubscription, UUID> {

  // 사용자가 특정 플레이리스트를 이미 구독 중인지 확인
  @Query(
      """
    select count(ps) > 0
    from PlaylistSubscription ps
    where ps.subscriber.id = :subscriberId
      and ps.playlist.id = :playlistId
      """)
  boolean existsBySubscriberIdAndPlaylistId(
      @Param("subscriberId") UUID subscriberId, @Param("playlistId") UUID playlistId);

  // 사용자와 플레이리스트 기준으로 구독 정보를 조회
  @Query(
      """
    select ps
    from PlaylistSubscription ps
    where ps.subscriber.id = :subscriberId
      and ps.playlist.id = :playlistId
      """)
  Optional<PlaylistSubscription> findBySubscriberIdAndPlaylistId(
      @Param("subscriberId") UUID subscriberId, @Param("playlistId") UUID playlistId);

  // 특정 플레이리스트의 구독자 수 조회
  @Query(
      """
    select count(ps)
    from PlaylistSubscription ps
    where ps.playlist.id = :playlistId
      """)
  long countByPlaylistId(@Param("playlistId") UUID playlistId);

  interface PlaylistSubscriptionCountProjection {

    UUID getPlaylistId();

    Long getSubscriberCount();
  }

  // playlistId 목록 기준으로 구독자 수를 한 번에 조회
  @Query(
      """
    select ps.playlist.id as playlistId, count(ps) as subscriberCount
    from PlaylistSubscription ps
    where ps.playlist.id in :playlistIds
    group by ps.playlist.id
      """)
  List<PlaylistSubscriptionCountProjection> countByPlaylistIds(
      @Param("playlistIds") Collection<UUID> playlistIds);

  // 현재 사용자가 구독한 playlistId 목록을 한 번에 조회
  @Query(
      """
    select ps.playlist.id
    from PlaylistSubscription ps
    where ps.subscriber.id = :subscriberId
      and ps.playlist.id in :playlistIds
      """)
  Set<UUID> findSubscribedPlaylistIds(
      @Param("subscriberId") UUID subscriberId, @Param("playlistIds") Collection<UUID> playlistIds);
}
