package com.sb10.mopl.conversation.repository;

import static com.sb10.mopl.conversation.entity.QDirectMessage.directMessage;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sb10.mopl.common.pagination.SortDirection;
import com.sb10.mopl.conversation.dto.DirectMessageSearchRequest;
import com.sb10.mopl.conversation.entity.DirectMessage;
import com.sb10.mopl.conversation.exception.ConversationErrorCode;
import com.sb10.mopl.conversation.exception.ConversationException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DirectMessageRepositoryCustomImpl implements DirectMessageRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  @Override
  public List<DirectMessage> search(UUID conversationId, DirectMessageSearchRequest request) {
    return queryFactory
        .selectFrom(directMessage)
        .join(directMessage.sender)
        .fetchJoin()
        .join(directMessage.receiver)
        .fetchJoin()
        .where(
            directMessage.conversation.id.eq(conversationId),
            cursorCondition(request.cursor(), request.idAfter(), request.sortDirection()))
        .orderBy(orderSpecifiers(request.sortDirection()))
        .limit(request.limit() + 1L)
        .fetch();
  }

  @Override
  public long countMessages(UUID conversationId) {
    Long count =
        queryFactory
            .select(directMessage.count())
            .from(directMessage)
            .where(directMessage.conversation.id.eq(conversationId))
            .fetchOne();

    return count != null ? count : 0L;
  }

  /**
   * 커서 기반 페이지네이션 조건절을 생성합니다.
   *
   * <p>DM 목록은 생성일(createdAt) 하나뿐이므로, createdAt과 id를 조합한 복합 커서로 다음 페이지를 판별합니다.
   */
  private BooleanExpression cursorCondition(
      String cursor, UUID idAfter, SortDirection sortDirection) {
    if (cursor == null || cursor.isBlank() || idAfter == null) {
      return null;
    }

    boolean isAsc = sortDirection == SortDirection.ASCENDING;
    try {
      Instant cursorTime = Instant.parse(cursor);
      return isAsc
          ? directMessage
              .createdAt
              .gt(cursorTime)
              .or(directMessage.createdAt.eq(cursorTime).and(directMessage.id.gt(idAfter)))
          : directMessage
              .createdAt
              .lt(cursorTime)
              .or(directMessage.createdAt.eq(cursorTime).and(directMessage.id.lt(idAfter)));
    } catch (DateTimeParseException e) {
      throw new ConversationException(
          ConversationErrorCode.INVALID_CURSOR_VALUE, Map.of("cursor", cursor), e);
    }
  }

  /** DM 목록은 createdAt과 id로 2차 정렬합니다. */
  private OrderSpecifier<?>[] orderSpecifiers(SortDirection sortDirection) {
    boolean isAsc = sortDirection == SortDirection.ASCENDING;
    return new OrderSpecifier<?>[] {
      isAsc ? directMessage.createdAt.asc() : directMessage.createdAt.desc(),
      isAsc ? directMessage.id.asc() : directMessage.id.desc()
    };
  }
}
