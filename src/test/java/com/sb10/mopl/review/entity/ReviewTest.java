package com.sb10.mopl.review.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.sb10.mopl.content.entity.Content;
import com.sb10.mopl.review.exception.ReviewErrorCode;
import com.sb10.mopl.review.exception.ReviewException;
import com.sb10.mopl.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReviewTest {

  private Content content;
  private User user;

  @BeforeEach
  void setUp() {
    content = mock(Content.class);
    user = mock(User.class);
  }

  @Test
  @DisplayName("콘텐츠, 작성자, 내용, 평점이 유효하면 Review를 생성한다")
  void create_success_whenReviewValuesAreValid() {
    // when
    Review review = new Review(content, user, "좋은 콘텐츠입니다.", 5);

    // then
    assertSame(content, review.getTargetContent());
    assertSame(user, review.getUser());
    assertEquals("좋은 콘텐츠입니다.", review.getText());
    assertEquals(5, review.getRating());
  }

  @Test
  @DisplayName("콘텐츠가 null이면 Review 생성에 실패한다")
  void create_throwInvalidValue_whenContentIsNull() {
    // when
    ReviewException exception =
      assertThrows(
        ReviewException.class,
        () -> new Review(null, user, "좋은 콘텐츠입니다.", 5));

    // then
    assertEquals(ReviewErrorCode.INVALID_REVIEW_VALUE, exception.getErrorCode());
    assertTrue(exception.getDetails().containsKey("contentId"));
  }

  @Test
  @DisplayName("작성자가 null이면 Review 생성에 실패한다")
  void create_throwInvalidValue_whenUserIsNull() {
    // when
    ReviewException exception =
      assertThrows(
        ReviewException.class,
        () -> new Review(content, null, "좋은 콘텐츠입니다.", 5));

    // then
    assertEquals(ReviewErrorCode.INVALID_REVIEW_VALUE, exception.getErrorCode());
    assertTrue(exception.getDetails().containsKey("userId"));
  }

  @Test
  @DisplayName("리뷰 내용이 null이면 Review 생성에 실패한다")
  void create_throwInvalidValue_whenTextIsNull() {
    // when
    ReviewException exception =
      assertThrows(
        ReviewException.class,
        () -> new Review(content, user, null, 5));

    // then
    assertEquals(ReviewErrorCode.INVALID_REVIEW_VALUE, exception.getErrorCode());
    assertTrue(exception.getDetails().containsKey("text"));
  }

  @Test
  @DisplayName("리뷰 내용이 공백이면 Review 생성에 실패한다")
  void create_throwInvalidValue_whenTextIsBlank() {
    // when
    ReviewException exception =
      assertThrows(
        ReviewException.class,
        () -> new Review(content, user, " ", 5));

    // then
    assertEquals(ReviewErrorCode.INVALID_REVIEW_VALUE, exception.getErrorCode());
    assertTrue(exception.getDetails().containsKey("text"));
  }

  @Test
  @DisplayName("평점이 null이면 Review 생성에 실패한다")
  void create_throwInvalidValue_whenRatingIsNull() {
    // when
    ReviewException exception =
      assertThrows(
        ReviewException.class,
        () -> new Review(content, user, "좋은 콘텐츠입니다.", null));

    // then
    assertEquals(ReviewErrorCode.INVALID_REVIEW_VALUE, exception.getErrorCode());
    assertTrue(exception.getDetails().containsKey("rating"));
  }

  @Test
  @DisplayName("평점이 1점 미만이면 Review 생성에 실패한다")
  void create_throwInvalidValue_whenRatingIsLessThanOne() {
    // when
    ReviewException exception =
      assertThrows(
        ReviewException.class,
        () -> new Review(content, user, "좋은 콘텐츠입니다.", 0));

    // then
    assertEquals(ReviewErrorCode.INVALID_REVIEW_VALUE, exception.getErrorCode());
    assertTrue(exception.getDetails().containsKey("rating"));
  }

  @Test
  @DisplayName("평점이 5점 초과이면 Review 생성에 실패한다")
  void create_throwInvalidValue_whenRatingIsGreaterThanFive() {
    // when
    ReviewException exception =
      assertThrows(
        ReviewException.class,
        () -> new Review(content, user, "좋은 콘텐츠입니다.", 6));

    // then
    assertEquals(ReviewErrorCode.INVALID_REVIEW_VALUE, exception.getErrorCode());
    assertTrue(exception.getDetails().containsKey("rating"));
  }

  @Test
  @DisplayName("수정할 리뷰 내용과 평점이 유효하면 Review를 수정한다")
  void update_success_whenReviewValuesAreValid() {
    // given
    Review review = new Review(content, user, "기존 리뷰", 3);

    // when
    review.update("수정된 리뷰", 5);

    // then
    assertEquals("수정된 리뷰", review.getText());
    assertEquals(5, review.getRating());
  }

  @Test
  @DisplayName("수정할 리뷰 내용이 null이면 Review 수정에 실패한다")
  void update_throwInvalidValue_whenTextIsNull() {
    // given
    Review review = new Review(content, user, "기존 리뷰", 3);

    // when
    ReviewException exception =
      assertThrows(ReviewException.class, () -> review.update(null, 5));

    // then
    assertEquals(ReviewErrorCode.INVALID_REVIEW_VALUE, exception.getErrorCode());
    assertTrue(exception.getDetails().containsKey("text"));
    assertEquals("기존 리뷰", review.getText());
    assertEquals(3, review.getRating());
  }

  @Test
  @DisplayName("수정할 리뷰 내용이 공백이면 Review 수정에 실패한다")
  void update_throwInvalidValue_whenTextIsBlank() {
    // given
    Review review = new Review(content, user, "기존 리뷰", 3);

    // when
    ReviewException exception =
      assertThrows(ReviewException.class, () -> review.update(" ", 5));

    // then
    assertEquals(ReviewErrorCode.INVALID_REVIEW_VALUE, exception.getErrorCode());
    assertTrue(exception.getDetails().containsKey("text"));
    assertEquals("기존 리뷰", review.getText());
    assertEquals(3, review.getRating());
  }

  @Test
  @DisplayName("수정할 평점이 null이면 Review 수정에 실패한다")
  void update_throwInvalidValue_whenRatingIsNull() {
    // given
    Review review = new Review(content, user, "기존 리뷰", 3);

    // when
    ReviewException exception =
      assertThrows(ReviewException.class, () -> review.update("수정된 리뷰", null));

    // then
    assertEquals(ReviewErrorCode.INVALID_REVIEW_VALUE, exception.getErrorCode());
    assertTrue(exception.getDetails().containsKey("rating"));
    assertEquals("기존 리뷰", review.getText());
    assertEquals(3, review.getRating());
  }

  @Test
  @DisplayName("수정할 평점이 1점 미만이면 Review 수정에 실패한다")
  void update_throwInvalidValue_whenRatingIsLessThanOne() {
    // given
    Review review = new Review(content, user, "기존 리뷰", 3);

    // when
    ReviewException exception =
      assertThrows(ReviewException.class, () -> review.update("수정된 리뷰", 0));

    // then
    assertEquals(ReviewErrorCode.INVALID_REVIEW_VALUE, exception.getErrorCode());
    assertTrue(exception.getDetails().containsKey("rating"));
    assertEquals("기존 리뷰", review.getText());
    assertEquals(3, review.getRating());
  }

  @Test
  @DisplayName("수정할 평점이 5점 초과이면 Review 수정에 실패한다")
  void update_throwInvalidValue_whenRatingIsGreaterThanFive() {
    // given
    Review review = new Review(content, user, "기존 리뷰", 3);

    // when
    ReviewException exception =
      assertThrows(ReviewException.class, () -> review.update("수정된 리뷰", 6));

    // then
    assertEquals(ReviewErrorCode.INVALID_REVIEW_VALUE, exception.getErrorCode());
    assertTrue(exception.getDetails().containsKey("rating"));
    assertEquals("기존 리뷰", review.getText());
    assertEquals(3, review.getRating());
  }

  @Test
  @DisplayName("여러 생성 값이 유효하지 않으면 각 필드의 검증 정보를 포함한다")
  void create_throwInvalidValue_whenMultipleValuesAreInvalid() {
    // when
    ReviewException exception =
      assertThrows(
        ReviewException.class,
        () -> new Review(null, null, " ", 0));

    // then
    assertEquals(ReviewErrorCode.INVALID_REVIEW_VALUE, exception.getErrorCode());
    assertTrue(exception.getDetails().containsKey("contentId"));
    assertTrue(exception.getDetails().containsKey("userId"));
    assertTrue(exception.getDetails().containsKey("text"));
    assertTrue(exception.getDetails().containsKey("rating"));
  }
}
