package com.sb10.mopl.notification.repository;

import static com.sb10.mopl.notification.entity.QNotification.notification;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sb10.mopl.common.pagination.SortDirection;
import com.sb10.mopl.notification.dto.NotificationSearchRequest;
import com.sb10.mopl.notification.entity.Notification;
import com.sb10.mopl.notification.exception.NotificationErrorCode;
import com.sb10.mopl.notification.exception.NotificationException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class NotificationRepositoryCustomImpl implements NotificationRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  // isRead갸 false인 알림만 반환합니다.
  @Override
  public List<Notification> search(UUID receiverId, NotificationSearchRequest request) {
    return queryFactory
        .selectFrom(notification)
        .where(
            notification.user.id.eq(receiverId),
            notification.isRead.isFalse(),
            cursorCondition(request.cursor(), request.idAfter(), request.sortDirection()))
        .orderBy(orderSpecifiers(request.sortDirection()))
        .limit(request.limit() + 1L)
        .fetch();
  }

  /** 커서 기반 페이지네이션 조건절(createdAt과 id 복합 커서로 다음 페이지를 판별) */
  private BooleanExpression cursorCondition(
      String cursor, UUID idAfter, SortDirection sortDirection) {
    boolean hasCursor = cursor != null && !cursor.isBlank();
    boolean hasIdAfter = idAfter != null;

    if (!hasCursor && !hasIdAfter) {
      return null;
    }
    if (hasCursor != hasIdAfter) {
      throw new NotificationException(
          NotificationErrorCode.INVALID_CURSOR_VALUE,
          Map.of("cursor", String.valueOf(cursor), "idAfter", String.valueOf(idAfter)));
    }

    boolean isAsc = sortDirection == SortDirection.ASCENDING;
    try {
      Instant cursorTime = Instant.parse(cursor);
      return isAsc
          ? notification
              .createdAt
              .gt(cursorTime)
              .or(notification.createdAt.eq(cursorTime).and(notification.id.gt(idAfter)))
          : notification
              .createdAt
              .lt(cursorTime)
              .or(notification.createdAt.eq(cursorTime).and(notification.id.lt(idAfter)));
    } catch (DateTimeParseException e) {
      throw new NotificationException(
          NotificationErrorCode.INVALID_CURSOR_VALUE, Map.of("cursor", cursor), e);
    }
  }

  /** 목록을 createdAt과 id로 2차 정렬 */
  private OrderSpecifier<?>[] orderSpecifiers(SortDirection sortDirection) {
    boolean isAsc = sortDirection == SortDirection.ASCENDING;
    return new OrderSpecifier<?>[] {
      isAsc ? notification.createdAt.asc() : notification.createdAt.desc(),
      isAsc ? notification.id.asc() : notification.id.desc()
    };
  }
}
