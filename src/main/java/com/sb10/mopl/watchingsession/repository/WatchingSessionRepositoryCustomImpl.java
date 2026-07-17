package com.sb10.mopl.watchingsession.repository;

import static com.sb10.mopl.watchingsession.entity.QWatchingSession.watchingSession;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sb10.mopl.common.pagination.SortDirection;
import com.sb10.mopl.watchingsession.dto.WatchingSessionSearchRequest;
import com.sb10.mopl.watchingsession.entity.WatchingSession;
import com.sb10.mopl.watchingsession.exception.WatchingSessionErrorCode;
import com.sb10.mopl.watchingsession.exception.WatchingSessionException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class WatchingSessionRepositoryCustomImpl implements WatchingSessionRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  @Override
  public List<WatchingSession> search(UUID contentId, WatchingSessionSearchRequest request) {
    return queryFactory
        .selectFrom(watchingSession)
        .join(watchingSession.watcher)
        .fetchJoin()
        .join(watchingSession.content)
        .fetchJoin()
        .where(
            watchingSession.content.id.eq(contentId),
            watcherNameLike(request.watcherNameLike()),
            cursorCondition(request.cursor(), request.idAfter(), request.sortDirection()))
        .orderBy(orderSpecifiers(request.sortDirection()))
        .limit(request.limit() + 1L)
        .fetch();
  }

  @Override
  public long countByContentId(UUID contentId, WatchingSessionSearchRequest request) {
    Long count =
        queryFactory
            .select(watchingSession.count())
            .from(watchingSession)
            .where(
                watchingSession.content.id.eq(contentId),
                watcherNameLike(request.watcherNameLike()))
            .fetchOne();

    return count != null ? count : 0L;
  }

  /** 시청자 이름(watcherNameLike) 검색 조건절을 생성합니다. null이거나 빈 문자열이면 조건절에서 생략됩니다. */
  private BooleanExpression watcherNameLike(String watcherNameLike) {
    if (watcherNameLike == null || watcherNameLike.isBlank()) {
      return null;
    }
    return watchingSession.watcher.name.contains(watcherNameLike.trim());
  }

  /**
   * 커서 기반 페이지네이션 조건절을 생성합니다.
   *
   * <p>시청 세션 목록은 생성일(createdAt) 하나뿐이므로, createdAt과 id를 조합한 복합 커서로 다음 페이지를 판별합니다.
   */
  private BooleanExpression cursorCondition(
      String cursor, UUID idAfter, SortDirection sortDirection) {
    boolean hasCursor = cursor != null && !cursor.isBlank();
    boolean hasIdAfter = idAfter != null;

    if (!hasCursor && !hasIdAfter) {
      return null;
    }
    if (hasCursor != hasIdAfter) {
      throw new WatchingSessionException(
          WatchingSessionErrorCode.INVALID_CURSOR_VALUE,
          Map.of("cursor", String.valueOf(cursor), "idAfter", String.valueOf(idAfter)));
    }

    boolean isAsc = sortDirection == SortDirection.ASCENDING;
    try {
      Instant cursorTime = Instant.parse(cursor);
      return isAsc
          ? watchingSession
              .createdAt
              .gt(cursorTime)
              .or(watchingSession.createdAt.eq(cursorTime).and(watchingSession.id.gt(idAfter)))
          : watchingSession
              .createdAt
              .lt(cursorTime)
              .or(watchingSession.createdAt.eq(cursorTime).and(watchingSession.id.lt(idAfter)));
    } catch (DateTimeParseException e) {
      throw new WatchingSessionException(
          WatchingSessionErrorCode.INVALID_CURSOR_VALUE, Map.of("cursor", cursor), e);
    }
  }

  /** 시청 세션 목록은 createdAt과 id로 2차 정렬합니다. */
  private OrderSpecifier<?>[] orderSpecifiers(SortDirection sortDirection) {
    boolean isAsc = sortDirection == SortDirection.ASCENDING;
    return new OrderSpecifier<?>[] {
      isAsc ? watchingSession.createdAt.asc() : watchingSession.createdAt.desc(),
      isAsc ? watchingSession.id.asc() : watchingSession.id.desc()
    };
  }
}
