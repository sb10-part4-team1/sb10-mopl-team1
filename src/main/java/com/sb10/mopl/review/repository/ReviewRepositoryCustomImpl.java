package com.sb10.mopl.review.repository;

import static com.sb10.mopl.review.entity.QReview.review;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sb10.mopl.review.entity.Review;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;

@RequiredArgsConstructor
public class ReviewRepositoryCustomImpl implements ReviewRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  // 특정 사용자가 콘텐츠에 작성한 리뷰가 이미 존재하는지 확인
  @Override
  public boolean existsByTargetContentIdAndUserId(UUID contentId, UUID userId) {
    Integer result =
        queryFactory
            .selectOne()
            .from(review)
            .where(review.targetContent.id.eq(contentId), review.user.id.eq(userId))
            .fetchFirst();

    return result != null;
  }

  // 콘텐츠 ID와 사용자 ID로 리뷰 조회
  @Override
  public Optional<Review> findByTargetContentIdAndUserId(UUID contentId, UUID userId) {

    Review result =
        queryFactory
            .selectFrom(review)
            .where(review.targetContent.id.eq(contentId), review.user.id.eq(userId))
            .fetchOne();

    return Optional.ofNullable(result);
  }

  // 콘텐츠와 커서 조건에 맞는 리뷰 목록 조회
  @Override
  public List<Review> findAllByCursorDesc(
      UUID contentId, Instant cursor, UUID idAfter, Pageable pageable) {

    return queryFactory
        .selectFrom(review)
        .join(review.user)
        .fetchJoin()
        .where(contentIdEqual(contentId), cursorCondition(cursor, idAfter))
        .orderBy(review.createdAt.desc(), review.id.asc())
        .offset(pageable.getOffset())
        .limit(pageable.getPageSize())
        .fetch();
  }

  // 특정 콘텐츠에 작성된 리뷰 개수 조회
  @Override
  public long countByTargetContentId(UUID contentId) {
    Long count =
        queryFactory
            .select(review.count())
            .from(review)
            .where(review.targetContent.id.eq(contentId))
            .fetchOne();

    return count != null ? count : 0L;
  }

  // 특정 콘텐츠의 리뷰만 조회하는 조건 생성
  private BooleanExpression contentIdEqual(UUID contentId) {
    return contentId != null ? review.targetContent.id.eq(contentId) : null;
  }

  // 생성 시각과 리뷰 ID를 기준으로 다음 페이지 커서 조건 생성
  private BooleanExpression cursorCondition(Instant cursor, UUID idAfter) {
    if (cursor == null) {
      return null;
    }

    BooleanExpression createdAtBeforeCursor = review.createdAt.lt(cursor);

    if (idAfter == null) {
      return createdAtBeforeCursor;
    }

    return createdAtBeforeCursor.or(review.createdAt.eq(cursor).and(review.id.gt(idAfter)));
  }
}
