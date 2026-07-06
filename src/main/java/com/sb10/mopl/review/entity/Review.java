package com.sb10.mopl.review.entity;

import com.sb10.mopl.common.entity.BaseUpdatableEntity;
import com.sb10.mopl.common.validation.DomainValidator;
import com.sb10.mopl.content.entity.Content;
import com.sb10.mopl.review.exception.ReviewErrorCode;
import com.sb10.mopl.review.exception.ReviewException;
import com.sb10.mopl.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
    name = "content_reviews",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "UQ_CONTENT_REVIEWS_USER",
          columnNames = {"content_id", "user_id"})
    })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review extends BaseUpdatableEntity {

  // JPA 연관관계 DB에 저장될때는 id만 저장
  // 리뷰 대상인 콘텐츠
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "content_id", nullable = false)
  private Content targetContent;

  // 리뷰 작성자
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  // 리뷰 본문
  @Column(name = "content", nullable = false, columnDefinition = "TEXT")
  private String text;

  // 리뷰 평점
  @Column(name = "rating", nullable = false)
  private Integer rating;

  private static void validateCreate(
      Content targetContent, User user, String text, Integer rating) {
    DomainValidator.start()
        .check(targetContent == null, "contentId", "리뷰 대상 콘텐츠는 필수입니다.")
        .check(user == null, "userId", "리뷰 작성자는 필수입니다.")
        .check(text == null || text.isBlank(), "text", "리뷰 내용은 필수입니다.")
        .check(rating == null, "rating", "평점은 필수입니다.")
        .check(rating != null && (rating < 1 || rating > 5), "rating", "평점은 1점 이상 5점 이하이어야 합니다.")
        .orThrow(details -> new ReviewException(ReviewErrorCode.INVALID_REVIEW_VALUE, details));
  }

  private static void validateUpdate(String text, Integer rating) {
    DomainValidator.start()
        .check(text == null || text.isBlank(), "text", "리뷰 내용은 필수입니다.")
        .check(rating == null, "rating", "평점은 필수입니다.")
        .check(rating != null && (rating < 1 || rating > 5), "rating", "평점은 1점 이상 5점 이하이어야 합니다.")
        .orThrow(details -> new ReviewException(ReviewErrorCode.INVALID_REVIEW_VALUE, details));
  }

  public Review(Content targetContent, User user, String text, Integer rating) {
    validateCreate(targetContent, user, text, rating);
    this.targetContent = targetContent;
    this.user = user;
    this.text = text;
    this.rating = rating;
  }

  public void update(String text, Integer rating) {
    validateUpdate(text, rating);
    this.text = text;
    this.rating = rating;
  }
}
