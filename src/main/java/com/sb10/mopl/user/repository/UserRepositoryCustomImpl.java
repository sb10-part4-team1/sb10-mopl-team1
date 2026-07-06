package com.sb10.mopl.user.repository;

import static com.sb10.mopl.user.entity.QUser.user;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.BooleanPath;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sb10.mopl.common.pagination.SortDirection;
import com.sb10.mopl.user.dto.request.UserSearchRequest;
import com.sb10.mopl.user.entity.User;
import com.sb10.mopl.user.entity.UserRole;
import com.sb10.mopl.user.exception.UserErrorCode;
import com.sb10.mopl.user.exception.UserException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UserRepositoryCustomImpl implements UserRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  @Override
  public List<User> findAllByCondition(UserSearchRequest request) {
    return queryFactory
        .selectFrom(user)
        .where(
            notDeleted(),
            emailLike(request.emailLike()),
            roleEqual(request.roleEqual()),
            isLocked(request.isLocked()),
            cursorCondition(request))
        .orderBy(orderSpecifiers(request))
        .limit((long) request.limit() + 1)
        .fetch();
  }

  @Override
  public long countByCondition(UserSearchRequest request) {
    Long count =
        queryFactory
            .select(user.count())
            .from(user)
            .where(
                notDeleted(),
                emailLike(request.emailLike()),
                roleEqual(request.roleEqual()),
                isLocked(request.isLocked()))
            .fetchOne();
    return count != null ? count : 0L;
  }

  private BooleanExpression notDeleted() {
    return user.isDeleted.isFalse();
  }

  private BooleanExpression emailLike(String email) {
    if (email == null || email.isBlank()) {
      return null;
    }
    return user.email.containsIgnoreCase(email.trim());
  }

  private BooleanExpression roleEqual(UserRole role) {
    return role != null ? user.role.eq(role) : null;
  }

  private BooleanExpression isLocked(Boolean locked) {
    return locked != null ? user.isLocked.eq(locked) : null;
  }

  private BooleanExpression cursorCondition(UserSearchRequest request) {
    if (request.cursor() == null || request.cursor().isBlank() || request.idAfter() == null) {
      return null;
    }

    boolean isAsc = request.sortDirection() == SortDirection.ASCENDING;
    UUID cursorId = request.idAfter();

    try {
      return switch (request.sortBy()) {
        case name -> stringCursorCondition(user.name, request.cursor(), cursorId, isAsc);
        case email -> stringCursorCondition(user.email, request.cursor(), cursorId, isAsc);
        case createdAt ->
            instantCursorCondition(
                user.createdAt, Instant.parse(request.cursor()), cursorId, isAsc);
        case isLocked ->
            booleanCursorCondition(
                user.isLocked, parseBooleanCursor(request.cursor()), cursorId, isAsc);
        case role ->
            stringCursorCondition(
                user.role.stringValue(), parseRoleCursor(request.cursor()).name(), cursorId, isAsc);
      };
    } catch (DateTimeParseException | IllegalArgumentException e) {
      throw new UserException(
          UserErrorCode.INVALID_USER_VALUE, Map.of("cursor", "올바르지 않은 커서 형식입니다."), e);
    }
  }

  private BooleanExpression stringCursorCondition(
      StringExpression path, String cursor, UUID cursorId, boolean isAsc) {
    return isAsc
        ? path.gt(cursor).or(path.eq(cursor).and(user.id.gt(cursorId)))
        : path.lt(cursor).or(path.eq(cursor).and(user.id.lt(cursorId)));
  }

  private BooleanExpression instantCursorCondition(
      DateTimePath<Instant> path, Instant cursor, UUID cursorId, boolean isAsc) {
    return isAsc
        ? path.gt(cursor).or(path.eq(cursor).and(user.id.gt(cursorId)))
        : path.lt(cursor).or(path.eq(cursor).and(user.id.lt(cursorId)));
  }

  private BooleanExpression booleanCursorCondition(
      BooleanPath path, boolean cursor, UUID cursorId, boolean isAsc) {
    if (isAsc) {
      return cursor
          ? path.isTrue().and(user.id.gt(cursorId))
          : path.isTrue().or(path.isFalse().and(user.id.gt(cursorId)));
    }
    return cursor
        ? path.isFalse().or(path.isTrue().and(user.id.lt(cursorId)))
        : path.isFalse().and(user.id.lt(cursorId));
  }

  private Boolean parseBooleanCursor(String cursor) {
    String normalized = cursor.toLowerCase(Locale.ROOT);
    if ("true".equals(normalized)) {
      return true;
    }
    if ("false".equals(normalized)) {
      return false;
    }
    throw new IllegalArgumentException("Invalid boolean cursor: " + cursor);
  }

  private UserRole parseRoleCursor(String cursor) {
    return UserRole.valueOf(cursor.toUpperCase(Locale.ROOT));
  }

  private OrderSpecifier<?>[] orderSpecifiers(UserSearchRequest request) {
    boolean isAsc = request.sortDirection() == SortDirection.ASCENDING;

    OrderSpecifier<?> primary =
        switch (request.sortBy()) {
          case name -> isAsc ? user.name.asc() : user.name.desc();
          case email -> isAsc ? user.email.asc() : user.email.desc();
          case createdAt -> isAsc ? user.createdAt.asc() : user.createdAt.desc();
          case isLocked -> isAsc ? user.isLocked.asc() : user.isLocked.desc();
          case role -> isAsc ? user.role.stringValue().asc() : user.role.stringValue().desc();
        };

    return new OrderSpecifier<?>[] {primary, isAsc ? user.id.asc() : user.id.desc()};
  }
}
