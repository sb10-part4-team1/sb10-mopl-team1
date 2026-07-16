package com.sb10.mopl.review.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.sb10.mopl.config.JpaAuditingConfig;
import com.sb10.mopl.config.QuerydslConfig;
import com.sb10.mopl.content.entity.Content;
import com.sb10.mopl.content.entity.ContentType;
import com.sb10.mopl.content.repository.ContentRepository;
import com.sb10.mopl.review.entity.Review;
import com.sb10.mopl.user.entity.User;
import com.sb10.mopl.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceUnitUtil;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaAuditingConfig.class, QuerydslConfig.class})
class ReviewRepositoryTest {

  private static final Pageable DEFAULT_PAGEABLE = PageRequest.of(0, 10);

  @Autowired
  private ReviewRepository reviewRepository;

  @Autowired
  private ContentRepository contentRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private EntityManager entityManager;

  private Content content;
  private Content otherContent;

  private User user;
  private User otherUser;
  private User thirdUser;

  @BeforeEach
  void setUp() {
    content =
      contentRepository.save(
        Content.create(
          "영화 콘텐츠",
          ContentType.MOVIE,
          "영화 콘텐츠 설명",
          "/uploads/movie.jpg"));

    otherContent =
      contentRepository.save(
        Content.create(
          "다른 콘텐츠",
          ContentType.MOVIE,
          "다른 콘텐츠 설명",
          "/uploads/other.jpg"));

    user = userRepository.save(createUser("사용자", "user@example.com"));
    otherUser = userRepository.save(createUser("다른 사용자", "other-user@example.com"));
    thirdUser = userRepository.save(createUser("세 번째 사용자", "third-user@example.com"));

    entityManager.flush();
    entityManager.clear();
  }

  @Test
  @DisplayName("사용자가 콘텐츠에 작성한 리뷰가 존재하면 true를 반환한다")
  void existsByTargetContentIdAndUserId_returnTrue_whenReviewExists() {
    // given
    reviewRepository.save(new Review(content, user, "좋은 콘텐츠입니다.", 5));

    entityManager.flush();
    entityManager.clear();

    // when
    boolean result =
      reviewRepository.existsByTargetContentIdAndUserId(
        content.getId(), user.getId());

    // then
    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("사용자가 콘텐츠에 작성한 리뷰가 존재하지 않으면 false를 반환한다")
  void existsByTargetContentIdAndUserId_returnFalse_whenReviewDoesNotExist() {
    // when
    boolean result =
      reviewRepository.existsByTargetContentIdAndUserId(
        content.getId(), user.getId());

    // then
    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("콘텐츠 ID와 사용자 ID로 리뷰를 조회한다")
  void findByTargetContentIdAndUserId_returnReview_whenReviewExists() {
    // given
    Review savedReview =
      reviewRepository.save(new Review(content, user, "좋은 콘텐츠입니다.", 5));

    entityManager.flush();
    entityManager.clear();

    // when
    Optional<Review> result =
      reviewRepository.findByTargetContentIdAndUserId(
        content.getId(), user.getId());

    // then
    assertThat(result).isPresent();
    assertThat(result.get().getId()).isEqualTo(savedReview.getId());
    assertThat(result.get().getTargetContent().getId()).isEqualTo(content.getId());
    assertThat(result.get().getUser().getId()).isEqualTo(user.getId());
    assertThat(result.get().getText()).isEqualTo("좋은 콘텐츠입니다.");
    assertThat(result.get().getRating()).isEqualTo(5);
  }

  @Test
  @DisplayName("콘텐츠와 사용자에 해당하는 리뷰가 없으면 빈 Optional을 반환한다")
  void findByTargetContentIdAndUserId_returnEmpty_whenReviewDoesNotExist() {
    // when
    Optional<Review> result =
      reviewRepository.findByTargetContentIdAndUserId(
        content.getId(), user.getId());

    // then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("특정 콘텐츠의 리뷰를 생성 시각 내림차순으로 조회한다")
  void findAllByCursorDesc_returnReviewsOrderedByCreatedAtDescending() {
    // given
    Review oldestReview =
      saveReview(
        content,
        user,
        "가장 오래된 리뷰",
        3,
        Instant.parse("2026-07-14T00:00:00Z"));

    Review middleReview =
      saveReview(
        content,
        otherUser,
        "중간 리뷰",
        4,
        Instant.parse("2026-07-15T00:00:00Z"));

    Review newestReview =
      saveReview(
        content,
        thirdUser,
        "가장 최근 리뷰",
        5,
        Instant.parse("2026-07-16T00:00:00Z"));

    saveReview(
      otherContent,
      user,
      "다른 콘텐츠 리뷰",
      5,
      Instant.parse("2026-07-17T00:00:00Z"));

    // when
    List<Review> result =
      reviewRepository.findAllByCursorDesc(
        content.getId(), null, null, DEFAULT_PAGEABLE);

    // then
    assertThat(result)
      .extracting(Review::getId)
      .containsExactly(
        newestReview.getId(),
        middleReview.getId(),
        oldestReview.getId());
  }

  @Test
  @DisplayName("콘텐츠 ID가 없으면 모든 콘텐츠의 리뷰를 조회한다")
  void findAllByCursorDesc_returnAll_whenContentIdIsNull() {
    // given
    Review contentReview =
      saveReview(
        content,
        user,
        "콘텐츠 리뷰",
        5,
        Instant.parse("2026-07-15T00:00:00Z"));

    Review otherContentReview =
      saveReview(
        otherContent,
        otherUser,
        "다른 콘텐츠 리뷰",
        4,
        Instant.parse("2026-07-16T00:00:00Z"));

    // when
    List<Review> result =
      reviewRepository.findAllByCursorDesc(
        null, null, null, DEFAULT_PAGEABLE);

    // then
    assertThat(result)
      .extracting(Review::getId)
      .containsExactly(
        otherContentReview.getId(),
        contentReview.getId());
  }

  @Test
  @DisplayName("커서보다 먼저 생성된 리뷰를 조회한다")
  void findAllByCursorDesc_returnReviewsBeforeCursor() {
    // given
    Review oldestReview =
      saveReview(
        content,
        user,
        "가장 오래된 리뷰",
        3,
        Instant.parse("2026-07-14T00:00:00Z"));

    Review cursorReview =
      saveReview(
        content,
        otherUser,
        "커서 기준 리뷰",
        4,
        Instant.parse("2026-07-15T00:00:00Z"));

    saveReview(
      content,
      thirdUser,
      "가장 최근 리뷰",
      5,
      Instant.parse("2026-07-16T00:00:00Z"));

    // when
    List<Review> result =
      reviewRepository.findAllByCursorDesc(
        content.getId(),
        Instant.parse("2026-07-15T00:00:00Z"),
        cursorReview.getId(),
        DEFAULT_PAGEABLE);

    // then
    assertThat(result)
      .extracting(Review::getId)
      .containsExactly(oldestReview.getId());
  }

  @Test
  @DisplayName("페이지 크기만큼 리뷰를 조회한다")
  void findAllByCursorDesc_applyPageSize() {
    // given
    saveReview(
      content,
      user,
      "첫 번째 리뷰",
      3,
      Instant.parse("2026-07-14T00:00:00Z"));

    saveReview(
      content,
      otherUser,
      "두 번째 리뷰",
      4,
      Instant.parse("2026-07-15T00:00:00Z"));

    saveReview(
      content,
      thirdUser,
      "세 번째 리뷰",
      5,
      Instant.parse("2026-07-16T00:00:00Z"));

    Pageable pageable = PageRequest.of(0, 2);

    // when
    List<Review> result =
      reviewRepository.findAllByCursorDesc(
        content.getId(), null, null, pageable);

    // then
    assertThat(result).hasSize(2);
  }

  @Test
  @DisplayName("리뷰 목록 조회 시 작성자를 함께 조회한다")
  void findAllByCursorDesc_fetchJoinUser() {
    // given
    saveReview(
      content,
      user,
      "좋은 콘텐츠입니다.",
      5,
      Instant.parse("2026-07-16T00:00:00Z"));

    // when
    List<Review> result =
      reviewRepository.findAllByCursorDesc(
        content.getId(), null, null, DEFAULT_PAGEABLE);

    // then
    assertThat(result).hasSize(1);

    PersistenceUnitUtil persistenceUnitUtil =
      entityManager.getEntityManagerFactory().getPersistenceUnitUtil();

    assertThat(persistenceUnitUtil.isLoaded(result.get(0).getUser())).isTrue();
  }

  @Test
  @DisplayName("특정 콘텐츠에 작성된 리뷰 개수를 반환한다")
  void countByTargetContentId_returnReviewCount() {
    // given
    reviewRepository.saveAll(
      List.of(
        new Review(content, user, "첫 번째 리뷰", 3),
        new Review(content, otherUser, "두 번째 리뷰", 4),
        new Review(otherContent, thirdUser, "다른 콘텐츠 리뷰", 5)));

    entityManager.flush();
    entityManager.clear();

    // when
    long result =
      reviewRepository.countByTargetContentId(content.getId());

    // then
    assertThat(result).isEqualTo(2L);
  }

  @Test
  @DisplayName("콘텐츠에 작성된 리뷰가 없으면 0을 반환한다")
  void countByTargetContentId_returnZero_whenReviewDoesNotExist() {
    // when
    long result =
      reviewRepository.countByTargetContentId(content.getId());

    // then
    assertThat(result).isZero();
  }

  // 생성 시각을 지정하여 테스트용 리뷰 저장
  private Review saveReview(
    Content targetContent,
    User reviewUser,
    String text,
    Integer rating,
    Instant createdAt) {

    Review review =
      reviewRepository.save(
        new Review(targetContent, reviewUser, text, rating));

    entityManager.flush();

    entityManager
      .createQuery(
        "update Review review "
          + "set review.createdAt = :createdAt "
          + "where review.id = :reviewId")
      .setParameter("createdAt", createdAt)
      .setParameter("reviewId", review.getId())
      .executeUpdate();

    entityManager.clear();

    return review;
  }

  // 테스트용 사용자 생성
  private User createUser(String name, String email) {
    return User.createUser(name, email, "password", null);
  }
}
