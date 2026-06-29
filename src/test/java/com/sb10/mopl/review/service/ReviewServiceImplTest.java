package com.sb10.mopl.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.sb10.mopl.common.pagination.CursorPageResponse;
import com.sb10.mopl.common.pagination.SortDirection;
import com.sb10.mopl.content.entity.Content;
import com.sb10.mopl.content.exception.ContentException;
import com.sb10.mopl.content.repository.ContentRepository;
import com.sb10.mopl.review.dto.ReviewAuthorDto;
import com.sb10.mopl.review.dto.ReviewCreateRequest;
import com.sb10.mopl.review.dto.ReviewDto;
import com.sb10.mopl.review.dto.ReviewUpdateRequest;
import com.sb10.mopl.review.entity.Review;
import com.sb10.mopl.review.exception.ReviewErrorCode;
import com.sb10.mopl.review.exception.ReviewException;
import com.sb10.mopl.review.mapper.ReviewMapper;
import com.sb10.mopl.review.repository.ReviewRepository;
import com.sb10.mopl.user.entity.User;
import com.sb10.mopl.user.exception.UserException;
import com.sb10.mopl.user.repository.UserRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

  @Mock private ReviewRepository reviewRepository;

  @Mock private ReviewMapper reviewMapper;

  @Mock private ContentRepository contentRepository;

  @Mock private UserRepository userRepository;

  @InjectMocks private ReviewServiceImpl reviewService;

  private static ValidatorFactory validatorFactory;
  private static Validator validator;

  private UUID contentId;
  private UUID userId;
  private UUID reviewId;
  private Content content;
  private User user;
  private Review review;

  @BeforeAll
  static void setUpValidator() {
    validatorFactory = Validation.buildDefaultValidatorFactory();
    validator = validatorFactory.getValidator();
  }

  @AfterAll
  static void tearDownValidator() {
    validatorFactory.close();
  }

  @BeforeEach
  void setUp() {
    contentId = UUID.randomUUID();
    userId = UUID.randomUUID();
    reviewId = UUID.randomUUID();

    content = mock(Content.class);
    user = createUser(userId);
    review = createReview(reviewId, content, user);
  }

  @Test
  @DisplayName("리뷰 - 생성 성공")
  void create_success() {
    // given
    ReviewCreateRequest request = new ReviewCreateRequest(contentId, "좋은 콘텐츠입니다.", 5);
    ReviewDto expectedResponse = createReviewDto(reviewId, contentId, userId, request.text(), 5);

    given(contentRepository.findById(contentId)).willReturn(Optional.of(content));
    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(reviewRepository.existsByTargetContentIdAndUserId(contentId, userId)).willReturn(false);
    given(reviewMapper.toEntity(request, content, user)).willReturn(review);
    given(reviewRepository.saveAndFlush(review)).willReturn(review);
    given(reviewMapper.toDto(review)).willReturn(expectedResponse);

    // when
    ReviewDto result = reviewService.create(request, userId);

    // then
    assertThat(result).isEqualTo(expectedResponse);
    verify(reviewRepository).saveAndFlush(review);
  }

  @Test
  @DisplayName("리뷰 - 생성 실패 - 콘텐츠가 존재하지 않음")
  void create_fail_contentNotFound() {
    // given
    ReviewCreateRequest request = new ReviewCreateRequest(contentId, "좋은 콘텐츠입니다.", 5);

    given(contentRepository.findById(contentId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> reviewService.create(request, userId))
        .isInstanceOf(ContentException.class);

    verify(userRepository, never()).findById(any());
    verify(reviewRepository, never()).saveAndFlush(any());
  }

  @Test
  @DisplayName("리뷰 - 생성 실패 - 사용자가 존재하지 않음")
  void create_fail_userNotFound() {
    // given
    ReviewCreateRequest request = new ReviewCreateRequest(contentId, "좋은 콘텐츠입니다.", 5);

    given(contentRepository.findById(contentId)).willReturn(Optional.of(content));
    given(userRepository.findById(userId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> reviewService.create(request, userId))
        .isInstanceOf(UserException.class);

    verify(reviewRepository, never()).saveAndFlush(any());
  }

  @Test
  @DisplayName("리뷰 - 생성 실패 - 이미 해당 콘텐츠에 리뷰를 작성함")
  void create_fail_alreadyExists() {
    // given
    ReviewCreateRequest request = new ReviewCreateRequest(contentId, "좋은 콘텐츠입니다.", 5);

    given(contentRepository.findById(contentId)).willReturn(Optional.of(content));
    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(reviewRepository.existsByTargetContentIdAndUserId(contentId, userId)).willReturn(true);

    // when & then
    assertThatThrownBy(() -> reviewService.create(request, userId))
        .isInstanceOf(ReviewException.class)
        .extracting("errorCode")
        .isEqualTo(ReviewErrorCode.REVIEW_ALREADY_EXISTS);

    verify(reviewRepository, never()).saveAndFlush(any());
  }

  @Test
  @DisplayName("리뷰 - 생성 실패 - 동시 요청으로 유니크 제약 위반 발생")
  void create_fail_dataIntegrityViolation() {
    // given
    ReviewCreateRequest request = new ReviewCreateRequest(contentId, "좋은 콘텐츠입니다.", 5);

    given(contentRepository.findById(contentId)).willReturn(Optional.of(content));
    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(reviewRepository.existsByTargetContentIdAndUserId(contentId, userId)).willReturn(false);
    given(reviewMapper.toEntity(request, content, user)).willReturn(review);
    given(reviewRepository.saveAndFlush(review))
        .willThrow(new DataIntegrityViolationException("duplicate review"));

    // when & then
    assertThatThrownBy(() -> reviewService.create(request, userId))
        .isInstanceOf(ReviewException.class)
        .extracting("errorCode")
        .isEqualTo(ReviewErrorCode.REVIEW_ALREADY_EXISTS);
  }

  @Test
  @DisplayName("리뷰 - 생성 실패 - 평점이 1점 미만")
  void createRequest_fail_ratingLessThanMin() {
    // given
    ReviewCreateRequest request = new ReviewCreateRequest(contentId, "좋은 콘텐츠입니다.", 0);

    // when
    Set<ConstraintViolation<ReviewCreateRequest>> violations = validator.validate(request);

    // then
    assertThat(violations)
        .anyMatch(violation -> violation.getPropertyPath().toString().equals("rating"));
  }

  @Test
  @DisplayName("리뷰 - 생성 실패 - 평점이 5점 초과")
  void createRequest_fail_ratingGreaterThanMax() {
    // given
    ReviewCreateRequest request = new ReviewCreateRequest(contentId, "좋은 콘텐츠입니다.", 6);

    // when
    Set<ConstraintViolation<ReviewCreateRequest>> violations = validator.validate(request);

    // then
    assertThat(violations)
        .anyMatch(violation -> violation.getPropertyPath().toString().equals("rating"));
  }

  @Test
  @DisplayName("리뷰 - 생성 실패 - 의견이 비어 있음")
  void createRequest_fail_blankText() {
    // given
    ReviewCreateRequest request = new ReviewCreateRequest(contentId, " ", 5);

    // when
    Set<ConstraintViolation<ReviewCreateRequest>> violations = validator.validate(request);

    // then
    assertThat(violations)
        .anyMatch(violation -> violation.getPropertyPath().toString().equals("text"));
  }

  @Test
  @DisplayName("리뷰 - 수정 실패 - 의견이 비어 있음")
  void updateRequest_fail_blankText() {
    // given
    ReviewUpdateRequest request = new ReviewUpdateRequest(" ", 5);

    // when
    Set<ConstraintViolation<ReviewUpdateRequest>> violations = validator.validate(request);

    // then
    assertThat(violations)
        .anyMatch(violation -> violation.getPropertyPath().toString().equals("text"));
  }

  @Test
  @DisplayName("리뷰 - 수정 성공")
  void update_success() {
    // given
    ReviewUpdateRequest request = new ReviewUpdateRequest("수정된 리뷰", 5);
    ReviewDto expectedResponse = createReviewDto(reviewId, contentId, userId, request.text(), 5);

    given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));
    given(reviewMapper.toDto(review)).willReturn(expectedResponse);

    // when
    ReviewDto result = reviewService.update(reviewId, request, userId);

    // then
    assertThat(result).isEqualTo(expectedResponse);
    assertThat(review.getText()).isEqualTo("수정된 리뷰");
    assertThat(review.getRating()).isEqualTo(5);
  }

  @Test
  @DisplayName("리뷰 - 수정 실패 - 리뷰가 존재하지 않음")
  void update_fail_reviewNotFound() {
    // given
    ReviewUpdateRequest request = new ReviewUpdateRequest("수정된 리뷰", 5);

    given(reviewRepository.findById(reviewId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> reviewService.update(reviewId, request, userId))
        .isInstanceOf(ReviewException.class)
        .extracting("errorCode")
        .isEqualTo(ReviewErrorCode.REVIEW_NOT_FOUND);
  }

  @Test
  @DisplayName("리뷰 - 수정 실패 - 작성자가 아님")
  void update_fail_unauthorized() {
    // given
    UUID otherUserId = UUID.randomUUID();
    ReviewUpdateRequest request = new ReviewUpdateRequest("수정된 리뷰", 5);

    given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));

    // when & then
    assertThatThrownBy(() -> reviewService.update(reviewId, request, otherUserId))
        .isInstanceOf(ReviewException.class)
        .extracting("errorCode")
        .isEqualTo(ReviewErrorCode.UNAUTHORIZED_REVIEW_ACCESS);
  }

  @Test
  @DisplayName("리뷰 - 삭제 성공")
  void delete_success() {
    // given
    given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));

    // when
    reviewService.delete(reviewId, userId);

    // then
    verify(reviewRepository).delete(review);
  }

  @Test
  @DisplayName("리뷰 - 삭제 실패 - 리뷰가 존재하지 않음")
  void delete_fail_reviewNotFound() {
    // given
    given(reviewRepository.findById(reviewId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> reviewService.delete(reviewId, userId))
        .isInstanceOf(ReviewException.class)
        .extracting("errorCode")
        .isEqualTo(ReviewErrorCode.REVIEW_NOT_FOUND);

    verify(reviewRepository, never()).delete(any());
  }

  @Test
  @DisplayName("리뷰 - 삭제 실패 - 작성자가 아님")
  void delete_fail_unauthorized() {
    // given
    UUID otherUserId = UUID.randomUUID();

    given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));

    // when & then
    assertThatThrownBy(() -> reviewService.delete(reviewId, otherUserId))
        .isInstanceOf(ReviewException.class)
        .extracting("errorCode")
        .isEqualTo(ReviewErrorCode.UNAUTHORIZED_REVIEW_ACCESS);

    verify(reviewRepository, never()).delete(any());
  }

  @Test
  @DisplayName("리뷰 - 목록 조회 성공")
  void findAll_success() {
    // given
    ReviewDto reviewDto = createReviewDto(reviewId, contentId, userId, "기존 리뷰", 3);

    given(
            reviewRepository.findAllByCursorDesc(
                eq(contentId), isNull(), isNull(), any(Pageable.class)))
        .willReturn(List.of(review));
    given(reviewMapper.toDto(review)).willReturn(reviewDto);
    given(reviewRepository.countByTargetContentId(contentId)).willReturn(1L);

    // when
    CursorPageResponse<ReviewDto> result =
        reviewService.findAll(contentId, null, null, 10, "createdAt", SortDirection.DESCENDING);

    // then
    assertThat(result.data()).containsExactly(reviewDto);
    assertThat(result.hasNext()).isFalse();
    assertThat(result.totalCount()).isEqualTo(1L);
  }

  @Test
  @DisplayName("리뷰 - 목록 조회 실패 - cursor와 idAfter 중 하나만 전달")
  void findAll_fail_cursorAndIdAfterMismatch() {
    // given
    String cursor = Instant.now().toString();

    // when & then
    assertThatThrownBy(
            () ->
                reviewService.findAll(
                    null, cursor, null, 10, "createdAt", SortDirection.DESCENDING))
        .isInstanceOf(ReviewException.class)
        .extracting("errorCode")
        .isEqualTo(ReviewErrorCode.INVALID_REVIEW_VALUE);
  }

  @Test
  @DisplayName("리뷰 - 목록 조회 실패 - 지원하지 않는 정렬 기준")
  void findAll_fail_invalidSortBy() {
    // when & then
    assertThatThrownBy(
            () -> reviewService.findAll(null, null, null, 10, "rating", SortDirection.DESCENDING))
        .isInstanceOf(ReviewException.class)
        .extracting("errorCode")
        .isEqualTo(ReviewErrorCode.INVALID_REVIEW_VALUE);
  }

  @Test
  @DisplayName("리뷰 - 목록 조회 실패 - 지원하지 않는 정렬 방향")
  void findAll_fail_invalidSortDirection() {
    // when & then
    assertThatThrownBy(
            () -> reviewService.findAll(null, null, null, 10, "createdAt", SortDirection.ASCENDING))
        .isInstanceOf(ReviewException.class)
        .extracting("errorCode")
        .isEqualTo(ReviewErrorCode.INVALID_REVIEW_VALUE);
  }

  @Test
  @DisplayName("리뷰 - 목록 조회 실패 - limit이 1보다 작음")
  void findAll_fail_invalidLimitLessThanOne() {
    // when & then
    assertThatThrownBy(
            () -> reviewService.findAll(null, null, null, 0, "createdAt", SortDirection.DESCENDING))
        .isInstanceOf(ReviewException.class)
        .extracting("errorCode")
        .isEqualTo(ReviewErrorCode.INVALID_REVIEW_VALUE);
  }

  @Test
  @DisplayName("리뷰 - 목록 조회 실패 - limit이 최대값을 초과함")
  void findAll_fail_invalidLimitGreaterThanMax() {
    // when & then
    assertThatThrownBy(
            () ->
                reviewService.findAll(null, null, null, 101, "createdAt", SortDirection.DESCENDING))
        .isInstanceOf(ReviewException.class)
        .extracting("errorCode")
        .isEqualTo(ReviewErrorCode.INVALID_REVIEW_VALUE);
  }

  @Test
  @DisplayName("리뷰 - 목록 조회 실패 - cursor 형식이 올바르지 않음")
  void findAll_fail_invalidCursorFormat() {
    // given
    UUID idAfter = UUID.randomUUID();

    // when & then
    assertThatThrownBy(
            () ->
                reviewService.findAll(
                    null, "invalid-cursor", idAfter, 10, "createdAt", SortDirection.DESCENDING))
        .isInstanceOf(ReviewException.class)
        .extracting("errorCode")
        .isEqualTo(ReviewErrorCode.INVALID_REVIEW_VALUE);
  }

  // 테스트용 User 생성 후 id 주입
  private User createUser(UUID userId) {
    User user = User.createUser("테스트유저", "test@example.com", "password", null);
    ReflectionTestUtils.setField(user, "id", userId);
    return user;
  }

  // 테스트용 기본 Review 생성 후 id 주입
  private Review createReview(UUID reviewId, Content content, User user) {
    Review review = new Review(content, user, "기존 리뷰", 3);
    ReflectionTestUtils.setField(review, "id", reviewId);
    return review;
  }

  // 테스트 검증에 사용할 ReviewDto 생성
  private ReviewDto createReviewDto(
      UUID reviewId, UUID contentId, UUID userId, String text, Integer rating) {
    return new ReviewDto(
        reviewId, contentId, new ReviewAuthorDto(userId, "테스트유저", null), text, rating);
  }
}
