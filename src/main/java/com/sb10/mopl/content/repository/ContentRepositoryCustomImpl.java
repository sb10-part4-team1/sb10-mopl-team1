package com.sb10.mopl.content.repository;

import static com.sb10.mopl.content.entity.QContent.content;
import static com.sb10.mopl.content.entity.QContentTag.contentTag;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.ComparableExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sb10.mopl.common.pagination.SortDirection;
import com.sb10.mopl.content.dto.ContentSearchRequest;
import com.sb10.mopl.content.dto.ContentSortBy;
import com.sb10.mopl.content.entity.Content;
import com.sb10.mopl.content.entity.ContentType;
import com.sb10.mopl.content.exception.ContentErrorCode;
import com.sb10.mopl.content.exception.ContentException;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ContentRepositoryCustomImpl implements ContentRepositoryCustom {

  private final JPAQueryFactory queryFactory;
  private final EntityManager em;

  /**
   * 조건 필터링 및 정렬 기준에 부합하는 콘텐츠의 목록을 슬라이스(Slice) 단위로 조회합니다. 메인 쿼리를 직관적이고 선언적으로 배치하여 전체 구조 파악이 용이합니다.
   */
  @Override
  public List<Content> findAllByCondition(ContentSearchRequest request) {
    int limit = request.limit() != null ? request.limit() : 20;

    return queryFactory
        .selectFrom(content)
        .where(
            typeEqual(request.typeEqual()), // 1. 카테고리 필터
            keywordLike(request.keywordLike()), // 2. 키워드 검색 필터
            tagsIn(request.tagsIn()), // 3. 태그 검색 필터
            cursorCondition(request)) // 4. 다음 페이지 시작 커서 필터
        .orderBy(getOrderSpecifiers(request)) // 정렬 규칙 바인딩
        .limit(limit + 1) // 다음 페이지 존재 여부(hasNext) 판단을 위해 1개 추가 조회
        .fetch();
  }

  /** 필터링 조건에 부합하는 전체 콘텐츠의 개수를 조회합니다. (페이지네이션 응답의 totalCount 계산용) */
  @Override
  public long countContents(ContentSearchRequest request) {
    Long count =
        queryFactory
            .select(content.count())
            .from(content)
            .where(
                typeEqual(request.typeEqual()),
                keywordLike(request.keywordLike()),
                tagsIn(request.tagsIn()))
            .fetchOne();
    return count != null ? count : 0L;
  }

  /**
   * 콘텐츠 타입(type) 필터링 조건절을 생성합니다. - 클라이언트가 typeEqual 파라미터를 보냈을 때만 "where type = :typeEqual" 쿼리가
   * 생성됩니다. - null이면 조건절에서 완전히 무시(생략)됩니다.
   */
  private BooleanExpression typeEqual(ContentType type) {
    return type != null ? content.type.eq(type) : null;
  }

  /**
   * 제목(title) 또는 설명(description) 검색 키워드 매칭 조건절을 생성합니다. - 입력된 키워드가 존재하면 "where title like %keyword%
   * or description like %keyword%" 형태로 쿼리가 생성됩니다. - null이거나 빈 문자열이면 조건절에서 생략됩니다.
   */
  private BooleanExpression keywordLike(String keyword) {
    if (keyword == null || keyword.isBlank()) {
      return null;
    }
    String trimmed = keyword.trim();
    return content.title.contains(trimmed).or(content.description.contains(trimmed));
  }

  /**
   * 태그 조건절을 생성합니다. - N:M 다대다 관계 매핑 테이블을 효율적으로 조회하고 데이터 중복을 방지하기 위해 EXISTS 서브쿼리를 사용합니다. - "EXISTS
   * (SELECT 1 FROM ContentTag ct WHERE ct.content = c AND ct.tag.name IN :tags)" 형태로 쿼리가 생성됩니다.
   */
  private BooleanExpression tagsIn(List<String> tags) {
    if (tags == null || tags.isEmpty()) {
      return null;
    }
    return JPAExpressions.selectOne()
        .from(contentTag)
        .where(contentTag.content.eq(content).and(contentTag.tag.name.in(tags)))
        .exists();
  }

  /** 커서 기반 페이지네이션의 핵심 Where 조건절을 생성합니다. - 이전에 받아온 마지막 데이터의 고유 ID(idAfter)가 존재할 때만 작동합니다. */
  private BooleanExpression cursorCondition(ContentSearchRequest request) {
    // 1. 이전 페이지의 마지막 아이템 ID가 없는 첫 페이지 조회인 경우 커서 조건 적용 안 함
    if (request.idAfter() == null) {
      return null;
    }

    // 2. 기준점이 되는 이전 페이지의 마지막 콘텐츠 엔티티 정보를 데이터베이스에서 먼저 조회
    Content cursorContent = em.find(Content.class, request.idAfter());
    if (cursorContent == null) {
      throw new ContentException(ContentErrorCode.CONTENT_NOT_FOUND, Map.of());
    }

    boolean isAsc = request.sortDirection() == SortDirection.ASCENDING;
    ContentSortBy sortBy = request.sortBy();

    // 3. 각 정렬 케이스별 커서 조건식을 깔끔하게 매핑하여 반환
    return switch (sortBy) {
      case watcherCount -> getPopularCursorExpression(cursorContent, isAsc);
      case createdAt ->
          createCursorExpression(
              content.createdAt, cursorContent.getCreatedAt(), isAsc, cursorContent.getId());
      case rate ->
          createNumberCursorExpression(
              content.averageRating,
              cursorContent.getAverageRating(),
              isAsc,
              cursorContent.getId());
    };
  }

  /**
   * 인기순(POPULAR) 정렬 기준의 3중 복합 커서 조건식을 분기 및 생성합니다. - 1순위: 시청자 수 (watcherCount) - 2순위: 리뷰 수
   * (reviewCount) - 3순위: 고유 ID (id)
   */
  private BooleanExpression getPopularCursorExpression(Content cursorContent, boolean isAsc) {
    long cursorWatcher = cursorContent.getWatcherCount();
    int cursorReview = cursorContent.getReviewCount();
    UUID cursorId = cursorContent.getId();

    if (isAsc) {
      // 인기 오름차순(ASC) : 시청자 수가 크거나, (시청자 수는 같고 리뷰가 많거나), (둘 다 같고 ID가 크거나)
      return content
          .watcherCount
          .gt(cursorWatcher) // 시청자 수가 크거나
          .or(
              content
                  .watcherCount
                  .eq(cursorWatcher)
                  .and(content.reviewCount.gt(cursorReview))) // 시청자 수는 같고 리뷰 수가 크거나
          .or(
              content
                  .watcherCount
                  .eq(cursorWatcher) // 시청자 수는 같고
                  .and(content.reviewCount.eq(cursorReview)) // 리뷰 수는 같고
                  .and(content.id.gt(cursorId))); // id가 더 크거나
    } else {
      // 인기 내림차순(DESC) : 시청자 수가 작거나, (시청자 수는 같고 리뷰가 적거나), (둘 다 같고 ID가 작거나)
      return content
          .watcherCount
          .lt(cursorWatcher)
          .or(content.watcherCount.eq(cursorWatcher).and(content.reviewCount.lt(cursorReview)))
          .or(
              content
                  .watcherCount
                  .eq(cursorWatcher)
                  .and(content.reviewCount.eq(cursorReview))
                  .and(content.id.lt(cursorId)));
    }
  }

  /**
   * 일반 비교형 컬럼(예: 날짜인 createdAt)을 기준으로 복합 커서 조건식을 생성하는 제네릭 헬퍼 메서드입니다.
   *
   * @param path 정렬 기준이 되는 Querydsl의 컬럼 경로 (예: content.createdAt)
   * @param cursorVal 커서 엔티티가 가진 해당 컬럼의 실제 값 (예: cursorContent.getCreatedAt())
   * @param isAsc 오름차순 여부
   * @param cursorId 커서 엔티티의 고유 ID (UUID)
   * @param <T> Comparable을 구현한 비교 가능한 타입 (Instant 등)
   * @return Querydsl Where 절에 들어갈 BooleanExpression 조건식
   */
  private <T extends Comparable<?>> BooleanExpression createCursorExpression(
      ComparableExpression<T> path, T cursorVal, boolean isAsc, UUID cursorId) {
    if (isAsc) {
      // 오름차순 정렬일 때:
      // 1. 기준 컬럼의 값이 커서 값보다 크거나
      // 2. 기준 컬럼의 값이 커서 값과 같으면서, 고유 ID가 커서 ID보다 큰 경우
      return path.gt(cursorVal).or(path.eq(cursorVal).and(content.id.gt(cursorId)));
    } else {
      // 내림차순 정렬일 때:
      // 1. 기준 컬럼의 값이 커서 값보다 작거나
      // 2. 기준 컬럼의 값이 커서 값과 같으면서, 고유 ID가 커서 ID보다 작은 경우
      return path.lt(cursorVal).or(path.eq(cursorVal).and(content.id.lt(cursorId)));
    }
  }

  /**
   * 숫자형 컬럼(예: 평점인 averageRating 등)을 기준으로 복합 커서 조건식을 생성하는 제네릭 헬퍼 메서드입니다. Querydsl의 타입 계층상
   * 숫자(Number)와 일반 비교형(Comparable)의 gt/lt 구조가 달라 메서드 오버로딩으로 분리했습니다.
   *
   * @param path 정렬 기준이 되는 Querydsl의 숫자 컬럼 경로 (예: content.averageRating)
   * @param cursorVal 커서 엔티티가 가진 해당 컬럼의 실제 값 (예: cursorContent.getAverageRating())
   * @param isAsc 오름차순 여부
   * @param cursorId 커서 엔티티의 고유 ID (UUID)
   * @param <T> Number와 Comparable을 동시에 구현한 타입 (Double, Integer 등)
   * @return Querydsl Where 절에 들어갈 BooleanExpression 조건식
   */
  private <T extends Number & Comparable<?>> BooleanExpression createNumberCursorExpression(
      NumberExpression<T> path, T cursorVal, boolean isAsc, UUID cursorId) {
    if (isAsc) {
      // 오름차순 정렬일 때:
      // 1. 기준 컬럼의 값이 커서 값보다 크거나
      // 2. 기준 컬럼의 값이 커서 값과 같으면서, 고유 ID가 커서 ID보다 큰 경우
      return path.gt(cursorVal).or(path.eq(cursorVal).and(content.id.gt(cursorId)));
    } else {
      // 내림차순 정렬일 때:
      // 1. 기준 컬럼의 값이 커서 값보다 작거나
      // 2. 기준 컬럼의 값이 커서 값과 같으면서, 고유 ID가 커서 ID보다 작은 경우
      return path.lt(cursorVal).or(path.eq(cursorVal).and(content.id.lt(cursorId)));
    }
  }

  /**
   * 클라이언트가 보낸 정렬 기준(sortBy)과 방향(sortDirection)에 맞추어 ORDER BY 컬럼 배열을 구성합니다.
   *
   * <p>- POPULAR (인기순): watcherCount(시청자수) -> reviewCount(리뷰수) -> id(고유 ID) 순으로 정렬 - CREATED_AT
   * (생성일순): createdAt(생성일) -> id(고유 ID) 순으로 정렬 - RATING (평점순): averageRating(평점) -> id(고유 ID) 순으로
   * 정렬
   */
  private OrderSpecifier<?>[] getOrderSpecifiers(ContentSearchRequest request) {
    boolean isAsc = request.sortDirection() == SortDirection.ASCENDING;
    ContentSortBy sortBy = request.sortBy();

    if (sortBy == ContentSortBy.watcherCount) {
      return new OrderSpecifier<?>[] {
        isAsc ? content.watcherCount.asc() : content.watcherCount.desc(),
        isAsc ? content.reviewCount.asc() : content.reviewCount.desc(),
        isAsc ? content.id.asc() : content.id.desc()
      };
    } else if (sortBy == ContentSortBy.createdAt) {
      return new OrderSpecifier<?>[] {
        isAsc ? content.createdAt.asc() : content.createdAt.desc(),
        isAsc ? content.id.asc() : content.id.desc()
      };
    } else if (sortBy == ContentSortBy.rate) {
      return new OrderSpecifier<?>[] {
        isAsc ? content.averageRating.asc() : content.averageRating.desc(),
        isAsc ? content.id.asc() : content.id.desc()
      };
    }

    return new OrderSpecifier<?>[] {isAsc ? content.id.asc() : content.id.desc()};
  }
}
