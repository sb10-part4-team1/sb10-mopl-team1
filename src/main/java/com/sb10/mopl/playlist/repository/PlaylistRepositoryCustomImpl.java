package com.sb10.mopl.playlist.repository;

import static com.sb10.mopl.playlist.entity.QPlaylist.playlist;
import static com.sb10.mopl.playlistsubscription.entity.QPlaylistSubscription.playlistSubscription;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sb10.mopl.common.pagination.SortDirection;
import com.sb10.mopl.playlist.entity.Playlist;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;

@RequiredArgsConstructor
public class PlaylistRepositoryCustomImpl implements PlaylistRepositoryCustom {

  private static final String SORT_BY_UPDATED_AT = "updatedAt";
  private static final String SORT_BY_SUBSCRIBER_COUNT = "subscribeCount";

  private final JPAQueryFactory queryFactory;

  // 조회 조건과 정렬 기준에 맞는 플레이리스트 목록 조회
  @Override
  public List<Playlist> findAllByCondition(
      String keywordLike,
      UUID ownerId,
      UUID subscriberId,
      Instant updatedAtCursor,
      Long subscriberCountCursor,
      UUID idAfter,
      String sortBy,
      SortDirection sortDirection,
      Pageable pageable) {

    return queryFactory
        .selectFrom(playlist)
        .join(playlist.owner)
        .fetchJoin()
        .where(
            keywordLike(keywordLike),
            ownerIdEqual(ownerId),
            subscribedBy(subscriberId),
            cursorCondition(updatedAtCursor, subscriberCountCursor, idAfter, sortBy, sortDirection))
        .orderBy(getOrderSpecifiers(sortBy, sortDirection))
        .offset(pageable.getOffset())
        .limit(pageable.getPageSize())
        .fetch();
  }

  // 조회 조건에 맞는 전체 플레이리스트 개수 조회
  @Override
  public long countByCondition(String keywordLike, UUID ownerId, UUID subscriberId) {
    Long count =
        queryFactory
            .select(playlist.count())
            .from(playlist)
            .where(keywordLike(keywordLike), ownerIdEqual(ownerId), subscribedBy(subscriberId))
            .fetchOne();

    return count != null ? count : 0L;
  }

  // 플레이리스트 ID로 소유자 정보를 포함한 플레이리스트 조회
  @Override
  public Optional<Playlist> findByIdWithOwner(UUID playlistId) {
    Playlist result =
        queryFactory
            .selectFrom(playlist)
            .join(playlist.owner)
            .fetchJoin()
            .where(playlist.id.eq(playlistId))
            .fetchOne();

    return Optional.ofNullable(result);
  }

  // 제목 또는 설명에 검색어가 포함되는 조건 생성
  private BooleanExpression keywordLike(String keywordLike) {
    if (keywordLike == null || keywordLike.isBlank()) {
      return null;
    }

    String trimmedKeyword = keywordLike.trim();

    return playlist
        .title
        .containsIgnoreCase(trimmedKeyword)
        .or(playlist.description.containsIgnoreCase(trimmedKeyword));
  }

  // 특정 소유자의 플레이리스트만 조회하는 조건 생성
  private BooleanExpression ownerIdEqual(UUID ownerId) {
    return ownerId != null ? playlist.owner.id.eq(ownerId) : null;
  }

  // 특정 사용자가 구독한 플레이리스트만 조회하는 조건 생성
  private BooleanExpression subscribedBy(UUID subscriberId) {
    if (subscriberId == null) {
      return null;
    }

    return JPAExpressions.selectOne()
        .from(playlistSubscription)
        .where(
            playlistSubscription.playlist.eq(playlist),
            playlistSubscription.subscriber.id.eq(subscriberId))
        .exists();
  }

  // 정렬 기준에 맞는 커서 조건 생성
  private BooleanExpression cursorCondition(
      Instant updatedAtCursor,
      Long subscriberCountCursor,
      UUID idAfter,
      String sortBy,
      SortDirection sortDirection) {

    if (idAfter == null) {
      return null;
    }

    boolean isAscending = sortDirection == SortDirection.ASCENDING;

    if (SORT_BY_SUBSCRIBER_COUNT.equals(sortBy)) {
      return subscriberCountCursorCondition(subscriberCountCursor, idAfter, isAscending);
    }

    if (SORT_BY_UPDATED_AT.equals(sortBy)) {
      return updatedAtCursorCondition(updatedAtCursor, idAfter, isAscending);
    }

    return null;
  }

  // 수정 시각 기준 커서 조건 생성
  private BooleanExpression updatedAtCursorCondition(
      Instant updatedAtCursor, UUID idAfter, boolean isAscending) {

    if (updatedAtCursor == null) {
      return null;
    }

    BooleanExpression cursorCondition =
        isAscending
            ? playlist.updatedAt.gt(updatedAtCursor)
            : playlist.updatedAt.lt(updatedAtCursor);

    return cursorCondition.or(playlist.updatedAt.eq(updatedAtCursor).and(playlist.id.gt(idAfter)));
  }

  // 구독자 수 기준 커서 조건 생성
  private BooleanExpression subscriberCountCursorCondition(
      Long subscriberCountCursor, UUID idAfter, boolean isAscending) {

    if (subscriberCountCursor == null) {
      return null;
    }

    NumberExpression<Long> subscriberCount = subscriberCountExpression();

    BooleanExpression cursorCondition =
        isAscending
            ? subscriberCount.gt(subscriberCountCursor)
            : subscriberCount.lt(subscriberCountCursor);

    return cursorCondition.or(
        subscriberCount.eq(subscriberCountCursor).and(playlist.id.gt(idAfter)));
  }

  // 플레이리스트별 구독자 수 표현식 생성
  private NumberExpression<Long> subscriberCountExpression() {
    return Expressions.numberTemplate(
        Long.class,
        "({0})",
        JPAExpressions.select(playlistSubscription.count())
            .from(playlistSubscription)
            .where(playlistSubscription.playlist.eq(playlist)));
  }

  // 정렬 기준과 방향에 맞는 정렬 조건 생성
  private OrderSpecifier<?>[] getOrderSpecifiers(String sortBy, SortDirection sortDirection) {

    boolean isAscending = sortDirection == SortDirection.ASCENDING;

    if (SORT_BY_SUBSCRIBER_COUNT.equals(sortBy)) {
      NumberExpression<Long> subscriberCount = subscriberCountExpression();

      return new OrderSpecifier<?>[] {
        isAscending ? subscriberCount.asc() : subscriberCount.desc(), playlist.id.asc()
      };
    }

    if (SORT_BY_UPDATED_AT.equals(sortBy)) {
      return new OrderSpecifier<?>[] {
        isAscending ? playlist.updatedAt.asc() : playlist.updatedAt.desc(), playlist.id.asc()
      };
    }

    return new OrderSpecifier<?>[] {playlist.id.asc()};
  }
}
