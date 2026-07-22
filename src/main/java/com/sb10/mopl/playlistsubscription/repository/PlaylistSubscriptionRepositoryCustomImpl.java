package com.sb10.mopl.playlistsubscription.repository;

import static com.sb10.mopl.playlistsubscription.entity.QPlaylistSubscription.playlistSubscription;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sb10.mopl.playlistsubscription.entity.PlaylistSubscription;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PlaylistSubscriptionRepositoryCustomImpl
    implements PlaylistSubscriptionRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  // 사용자가 특정 플레이리스트를 이미 구독 중인지 확인
  @Override
  public boolean existsBySubscriberIdAndPlaylistId(UUID subscriberId, UUID playlistId) {

    Integer result =
        queryFactory
            .selectOne()
            .from(playlistSubscription)
            .where(
                playlistSubscription.subscriber.id.eq(subscriberId),
                playlistSubscription.playlist.id.eq(playlistId))
            .fetchFirst();

    return result != null;
  }

  // 사용자와 플레이리스트 기준으로 구독 정보 조회
  @Override
  public Optional<PlaylistSubscription> findBySubscriberIdAndPlaylistId(
      UUID subscriberId, UUID playlistId) {

    PlaylistSubscription result =
        queryFactory
            .selectFrom(playlistSubscription)
            .where(
                playlistSubscription.subscriber.id.eq(subscriberId),
                playlistSubscription.playlist.id.eq(playlistId))
            .fetchOne();

    return Optional.ofNullable(result);
  }

  // 특정 플레이리스트의 구독자 수 조회
  @Override
  public long countByPlaylistId(UUID playlistId) {
    Long count =
        queryFactory
            .select(playlistSubscription.count())
            .from(playlistSubscription)
            .where(playlistSubscription.playlist.id.eq(playlistId))
            .fetchOne();

    return count != null ? count : 0L;
  }

  // playlistId 목록 기준으로 구독자 수를 한 번에 조회
  @Override
  public List<PlaylistSubscriptionRepository.PlaylistSubscriptionCountProjection>
      countByPlaylistIds(Collection<UUID> playlistIds) {

    if (playlistIds == null || playlistIds.isEmpty()) {
      return List.of();
    }

    NumberExpression<Long> subscriberCount = playlistSubscription.count();

    List<Tuple> results =
        queryFactory
            .select(playlistSubscription.playlist.id, subscriberCount)
            .from(playlistSubscription)
            .where(playlistSubscription.playlist.id.in(playlistIds))
            .groupBy(playlistSubscription.playlist.id)
            .fetch();

    return results.stream()
        .<PlaylistSubscriptionRepository.PlaylistSubscriptionCountProjection>map(
            result ->
                new PlaylistSubscriptionCount(
                    result.get(playlistSubscription.playlist.id), result.get(subscriberCount)))
        .toList();
  }

  // 현재 사용자가 구독한 playlistId 목록을 한 번에 조회
  @Override
  public Set<UUID> findSubscribedPlaylistIds(UUID subscriberId, Collection<UUID> playlistIds) {

    if (playlistIds == null || playlistIds.isEmpty()) {
      return Set.of();
    }

    List<UUID> results =
        queryFactory
            .select(playlistSubscription.playlist.id)
            .from(playlistSubscription)
            .where(
                playlistSubscription.subscriber.id.eq(subscriberId),
                playlistSubscription.playlist.id.in(playlistIds))
            .fetch();

    return new HashSet<>(results);
  }

  // 특정 플레이리스트를 구독하는 사용자 ID 목록 조회
  @Override
  public List<UUID> findSubscriberIdsByPlaylistId(UUID playlistId, UUID idAfter, int limit) {

    return queryFactory
        .select(playlistSubscription.subscriber.id)
        .from(playlistSubscription)
        .where(playlistSubscription.playlist.id.eq(playlistId), subscriberIdAfter(idAfter))
        .orderBy(playlistSubscription.subscriber.id.asc())
        .limit(limit)
        .fetch();
  }

  // 플레이리스트별 구독자 수 조회 결과
  private record PlaylistSubscriptionCount(UUID playlistId, Long subscriberCount)
      implements PlaylistSubscriptionRepository.PlaylistSubscriptionCountProjection {

    @Override
    public UUID getPlaylistId() {
      return playlistId;
    }

    @Override
    public Long getSubscriberCount() {
      return subscriberCount;
    }
  }

  private BooleanExpression subscriberIdAfter(UUID idAfter) {
    return idAfter != null ? playlistSubscription.subscriber.id.gt(idAfter) : null;
  }
}
