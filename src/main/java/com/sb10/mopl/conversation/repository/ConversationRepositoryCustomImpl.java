package com.sb10.mopl.conversation.repository;

import static com.sb10.mopl.conversation.entity.QConversation.conversation;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sb10.mopl.common.pagination.SortDirection;
import com.sb10.mopl.conversation.dto.ConversationSearchRequest;
import com.sb10.mopl.conversation.entity.Conversation;
import com.sb10.mopl.conversation.entity.QConversationParticipant;
import com.sb10.mopl.conversation.exception.ConversationErrorCode;
import com.sb10.mopl.conversation.exception.ConversationException;
import com.sb10.mopl.user.entity.QUser;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ConversationRepositoryCustomImpl implements ConversationRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  @Override
  public List<Conversation> search(UUID myUserId, ConversationSearchRequest request) {
    QConversationParticipant meP = new QConversationParticipant("meP");
    QConversationParticipant otherP = new QConversationParticipant("otherP");
    QUser otherUser = new QUser("otherUser");

    BooleanBuilder where =
        new BooleanBuilder()
            .and(meP.user.id.eq(myUserId))
            .and(otherP.user.id.ne(myUserId))
            .and(keywordCondition(otherUser, request.keywordLike()))
            .and(cursorCondition(request.cursor(), request.idAfter(), request.sortDirection()));

    return queryFactory
        .selectDistinct(conversation)
        .from(conversation)
        .join(meP)
        .on(meP.conversation.eq(conversation))
        .join(otherP)
        .on(otherP.conversation.eq(conversation))
        .join(otherP.user, otherUser)
        .where(where)
        .orderBy(orderSpecifiers(request.sortDirection()))
        .limit(request.limit() + 1L)
        .fetch();
  }

  @Override
  public long countConversations(UUID myUserId, ConversationSearchRequest request) {
    QConversationParticipant meP = new QConversationParticipant("meP");
    QConversationParticipant otherP = new QConversationParticipant("otherP");
    QUser otherUser = new QUser("otherUser");

    Long count =
        queryFactory
            .select(conversation.countDistinct())
            .from(conversation)
            .join(meP)
            .on(meP.conversation.eq(conversation))
            .join(otherP)
            .on(otherP.conversation.eq(conversation))
            .join(otherP.user, otherUser)
            .where(
                meP.user.id.eq(myUserId),
                otherP.user.id.ne(myUserId),
                keywordCondition(otherUser, request.keywordLike()))
            .fetchOne();

    return count != null ? count : 0L;
  }

  /** 대화 상대 이름(otherUser.name) 검색어 조건절. keywordLike가 없으면 조건절에서 생략됩니다. */
  private BooleanExpression keywordCondition(QUser otherUser, String keywordLike) {
    if (keywordLike == null || keywordLike.isBlank()) {
      return null;
    }
    return otherUser.name.contains(keywordLike.trim());
  }

  /**
   * 커서 기반 페이지네이션 조건절을 생성합니다.
   *
   * <p>대화 목록의 정렬 기준은 생성일(createdAt) 하나뿐이므로, createdAt과 id를 조합한 복합 커서로 다음 페이지를 판별합니다.
   */
  private BooleanExpression cursorCondition(
      String cursor, UUID idAfter, SortDirection sortDirection) {
    boolean hasCursor = cursor != null && !cursor.isBlank();
    boolean hasIdAfter = idAfter != null;

    if (!hasCursor && !hasIdAfter) {
      return null;
    }
    if (hasCursor != hasIdAfter) {
      throw new ConversationException(
          ConversationErrorCode.INVALID_CURSOR_VALUE,
          Map.of("cursor", String.valueOf(cursor), "idAfter", String.valueOf(idAfter)));
    }

    boolean isAsc = sortDirection == SortDirection.ASCENDING;
    try {
      Instant cursorTime = Instant.parse(cursor);
      return isAsc
          ? conversation
              .createdAt
              .gt(cursorTime)
              .or(conversation.createdAt.eq(cursorTime).and(conversation.id.gt(idAfter)))
          : conversation
              .createdAt
              .lt(cursorTime)
              .or(conversation.createdAt.eq(cursorTime).and(conversation.id.lt(idAfter)));
    } catch (DateTimeParseException e) {
      throw new ConversationException(
          ConversationErrorCode.INVALID_CURSOR_VALUE, Map.of("cursor", cursor), e);
    }
  }

  /** 대화 목록은 createdAt과 id로 2차 정렬합니다. */
  private OrderSpecifier<?>[] orderSpecifiers(SortDirection sortDirection) {
    boolean isAsc = sortDirection == SortDirection.ASCENDING;
    return new OrderSpecifier<?>[] {
      isAsc ? conversation.createdAt.asc() : conversation.createdAt.desc(),
      isAsc ? conversation.id.asc() : conversation.id.desc()
    };
  }
}
