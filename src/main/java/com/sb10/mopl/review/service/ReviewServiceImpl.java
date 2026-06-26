package com.sb10.mopl.review.service;

import com.sb10.mopl.common.pagination.CursorPageResponse;
import com.sb10.mopl.common.pagination.SortDirection;
import com.sb10.mopl.content.entity.Content;
import com.sb10.mopl.content.exception.ContentErrorCode;
import com.sb10.mopl.content.exception.ContentException;
import com.sb10.mopl.content.repository.ContentRepository;
import com.sb10.mopl.review.dto.ReviewCreateRequest;
import com.sb10.mopl.review.dto.ReviewDto;
import com.sb10.mopl.review.dto.ReviewUpdateRequest;
import com.sb10.mopl.review.entity.Review;
import com.sb10.mopl.review.exception.ReviewErrorCode;
import com.sb10.mopl.review.exception.ReviewException;
import com.sb10.mopl.review.mapper.ReviewMapper;
import com.sb10.mopl.review.repository.ReviewRepository;
import com.sb10.mopl.user.entity.User;
import com.sb10.mopl.user.exception.UserErrorCode;
import com.sb10.mopl.user.exception.UserException;
import com.sb10.mopl.user.repository.UserRepository;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewServiceImpl implements ReviewService {

  private final ReviewRepository reviewRepository;
  private final ReviewMapper reviewMapper;
  private final ContentRepository contentRepository;
  private final UserRepository userRepository;

  // 리뷰 목록 조회 최대 limit
  private static final int MAX_REVIEW_PAGE_LIMIT = 100;

  @Override
  @Transactional
  public ReviewDto create(ReviewCreateRequest request, UUID userId) {
    // 요청에서 리뷰 대상 콘텐츠 ID 추출
    UUID contentId = request.contentId();

    // 리뷰 대상 콘텐츠 존재 여부 검증
    Content content =
        contentRepository
            .findById(contentId)
            .orElseThrow(
                () ->
                    new ContentException(
                        ContentErrorCode.CONTENT_NOT_FOUND, Map.of("contentId", contentId)));

    // 리뷰 작성자 존재 여부 검증
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(
                () -> new UserException(UserErrorCode.USER_NOT_FOUND, Map.of("userId", userId)));

    // 이미 해당 콘텐츠에 리뷰를 작성했는지 검증
    if (reviewRepository.existsByTargetContentIdAndUserId(contentId, userId)) {
      throw new ReviewException(
          ReviewErrorCode.REVIEW_ALREADY_EXISTS, Map.of("contentId", contentId, "userId", userId));
    }

    // 요청 DTO를 Review 엔티티로 변환
    Review review = reviewMapper.toEntity(request, content, user);

    try {
      // 리뷰 저장 후 즉시 flush하여 DB 유니크 제약 위반을 현재 try-catch 안에서 감지
      Review savedReview = reviewRepository.saveAndFlush(review);
      return reviewMapper.toDto(savedReview);
    } catch (DataIntegrityViolationException e) {
      // 동시 요청으로 유니크 제약이 발생한 경우 중복 리뷰 예외로 변환
      throw new ReviewException(
          ReviewErrorCode.REVIEW_ALREADY_EXISTS,
          Map.of("contentId", contentId, "userId", userId),
          e);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public CursorPageResponse<ReviewDto> findAll(
      UUID contentId,
      String cursor,
      UUID idAfter,
      Integer limit,
      String sortBy,
      SortDirection sortDirection) {

    // 목록 조회 요청 파라미터 검증
    validateFindAllRequest(cursor, idAfter, limit, sortBy, sortDirection);

    // 커서 문자열을 createdAt 기준 Instant로 변환
    Instant parsedCursor = parseCursor(cursor);

    // 다음 페이지 여부 확인을 위해 limit보다 1개 더 조회
    List<Review> reviews =
        reviewRepository.findAllByCursorDesc(
            contentId, parsedCursor, idAfter, PageRequest.of(0, limit + 1));

    // 조회 결과를 커서 페이지 응답으로 변환
    return toCursorPageResponse(reviews, contentId, limit, sortBy, sortDirection);
  }

  @Override
  @Transactional
  public ReviewDto update(UUID reviewId, ReviewUpdateRequest request, UUID userId) {
    // 리뷰 자체 존재 여부 검증
    Review review =
        reviewRepository
            .findById(reviewId)
            .orElseThrow(
                () ->
                    new ReviewException(
                        ReviewErrorCode.REVIEW_NOT_FOUND, Map.of("reviewId", reviewId)));
    // 리뷰 작성자 권한 검증
    if (!review.getUser().getId().equals(userId)) {
      throw new ReviewException(
          ReviewErrorCode.UNAUTHORIZED_REVIEW_ACCESS,
          Map.of("reviewId", reviewId, "userId", userId));
    }
    // 리뷰 업데이트
    review.update(request.text(), request.rating());

    return reviewMapper.toDto(review);
  }

  @Override
  @Transactional
  public void delete(UUID reviewId, UUID userId) {
    // 리뷰 자체 존재 여부 검증
    Review review =
        reviewRepository
            .findById(reviewId)
            .orElseThrow(
                () ->
                    new ReviewException(
                        ReviewErrorCode.REVIEW_NOT_FOUND, Map.of("reviewId", reviewId)));

    if (!review.getUser().getId().equals(userId)) {
      throw new ReviewException(
          ReviewErrorCode.UNAUTHORIZED_REVIEW_ACCESS,
          Map.of("reviewId", reviewId, "userId", userId));
    }

    reviewRepository.delete(review);
  }

  private void validateFindAllRequest(
      String cursor, UUID idAfter, Integer limit, String sortBy, SortDirection sortDirection) {
    // 커서와 idAfter는 함께 전달
    boolean hasCursor = cursor != null && !cursor.isBlank();
    boolean hasIdAfter = idAfter != null;

    if (hasCursor != hasIdAfter) {
      throw new ReviewException(
          ReviewErrorCode.INVALID_REVIEW_VALUE, Map.of("cursor", "cursor와 idAfter는 함께 전달되어야 합니다."));
    }

    // 정렬 기준이 createdAt인지 검증
    if (!"createdAt".equals(sortBy)) {
      throw new ReviewException(
          ReviewErrorCode.INVALID_REVIEW_VALUE, Map.of("sortBy", "지원하지 않는 정렬 기준입니다."));
    }

    // 정렬 방향이 DESCENDING인지 검증
    if (sortDirection != SortDirection.DESCENDING) {
      throw new ReviewException(
          ReviewErrorCode.INVALID_REVIEW_VALUE,
          Map.of("sortDirection", "현재는 DESCENDING 정렬만 지원합니다."));
    }

    // 요청 limit이 유효한지 검증
    if (limit == null || limit <= 0 || limit > MAX_REVIEW_PAGE_LIMIT) {
      throw new ReviewException(
          ReviewErrorCode.INVALID_REVIEW_VALUE,
          Map.of("limit", "limit은 1 이상 " + MAX_REVIEW_PAGE_LIMIT + " 이하여야 합니다."));
    }
  }

  private Instant parseCursor(String cursor) {
    // 커서가 비어 있으면 첫 페이지 조회로 처리
    if (cursor == null || cursor.isBlank()) {
      return null;
    }

    try {
      // ISO-8601 문자열을 Instant로 변환
      return Instant.parse(cursor);
    } catch (DateTimeParseException e) {
      throw new ReviewException(
          ReviewErrorCode.INVALID_REVIEW_VALUE, Map.of("cursor", "올바르지 않은 커서 형식입니다."), e);
    }
  }

  private CursorPageResponse<ReviewDto> toCursorPageResponse(
      List<Review> reviews,
      UUID contentId,
      Integer limit,
      String sortBy,
      SortDirection sortDirection) {

    // 조회 결과가 limit보다 많으면 다음 페이지가 존재
    boolean hasNext = reviews.size() > limit;

    // 실제 응답 데이터는 요청 limit만큼만 사용
    List<Review> pageReviews = hasNext ? reviews.subList(0, limit) : reviews;

    // Review 엔티티 목록을 응답 DTO 목록으로 변환
    List<ReviewDto> data = pageReviews.stream().map(reviewMapper::toDto).toList();

    // 다음 페이지 요청에 사용할 마지막 리뷰 추출
    Review lastReview =
        hasNext && !pageReviews.isEmpty() ? pageReviews.get(pageReviews.size() - 1) : null;

    // 전체 리뷰 개수 조회
    long totalCount =
        contentId == null
            ? reviewRepository.count()
            : reviewRepository.countByTargetContentId(contentId);

    // 커서 페이지 응답 생성
    return new CursorPageResponse<>(
        data,
        getNextCursor(lastReview),
        getNextIdAfter(lastReview),
        hasNext,
        totalCount,
        sortBy,
        sortDirection);
  }

  private String getNextCursor(Review lastReview) {
    // 마지막 리뷰가 없으면 다음 커서를 생성하지 않음
    return lastReview == null ? null : lastReview.getCreatedAt().toString();
  }

  private UUID getNextIdAfter(Review lastReview) {
    // 마지막 리뷰가 없으면 다음 보조 커서를 생성하지 않음
    return lastReview == null ? null : lastReview.getId();
  }
}
