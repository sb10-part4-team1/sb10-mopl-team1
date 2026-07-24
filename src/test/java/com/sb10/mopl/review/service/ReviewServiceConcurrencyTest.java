package com.sb10.mopl.review.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sb10.mopl.content.entity.Content;
import com.sb10.mopl.content.entity.ContentType;
import com.sb10.mopl.content.repository.ContentRepository;
import com.sb10.mopl.review.dto.ReviewCreateRequest;
import com.sb10.mopl.review.dto.ReviewDto;
import com.sb10.mopl.review.dto.ReviewUpdateRequest;
import com.sb10.mopl.review.entity.Review;
import com.sb10.mopl.review.repository.ReviewRepository;
import com.sb10.mopl.user.entity.User;
import com.sb10.mopl.user.repository.UserRepository;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
class ReviewServiceConcurrencyTest {

  @Autowired private ReviewServiceImpl reviewService;

  @Autowired private ReviewRepository reviewRepository;

  @Autowired private ContentRepository contentRepository;

  @Autowired private UserRepository userRepository;

  @Autowired private TransactionTemplate transactionTemplate;

  private UUID contentId;
  private UUID firstUserId;
  private UUID secondUserId;

  @BeforeEach
  void setUp() {
    transactionTemplate.executeWithoutResult(
        status -> {
          String suffix = UUID.randomUUID().toString();

          User firstUser =
              userRepository.save(
                  User.createUser(
                      "첫 번째 사용자",
                      "review-concurrency-first-" + suffix + "@example.com",
                      "password",
                      null));

          User secondUser =
              userRepository.save(
                  User.createUser(
                      "두 번째 사용자",
                      "review-concurrency-second-" + suffix + "@example.com",
                      "password",
                      null));

          Content content =
              contentRepository.save(
                  Content.create(
                      "동시성 테스트 콘텐츠",
                      ContentType.MOVIE,
                      "동시성 테스트 설명",
                      "/uploads/review-concurrency.jpg"));

          firstUserId = firstUser.getId();
          secondUserId = secondUser.getId();
          contentId = content.getId();
        });
  }

  @AfterEach
  void tearDown() {
    transactionTemplate.executeWithoutResult(
        status -> {
          List<Review> reviews =
              reviewRepository.findAll().stream()
                  .filter(review -> review.getTargetContent().getId().equals(contentId))
                  .toList();

          reviewRepository.deleteAll(reviews);

          if (contentRepository.existsById(contentId)) {
            contentRepository.deleteById(contentId);
          }
          if (userRepository.existsById(firstUserId)) {
            userRepository.deleteById(firstUserId);
          }
          if (userRepository.existsById(secondUserId)) {
            userRepository.deleteById(secondUserId);
          }
        });
  }

  @Test
  @DisplayName("동일 콘텐츠에 리뷰를 동시에 생성해도 최종 통계가 일치한다")
  void concurrentCreate_updatesStatisticsCorrectly() throws Exception {
    ReviewCreateRequest firstRequest = new ReviewCreateRequest(contentId, "첫 번째 동시 리뷰", 5);
    ReviewCreateRequest secondRequest = new ReviewCreateRequest(contentId, "두 번째 동시 리뷰", 3);

    runConcurrently(
        () -> reviewService.create(firstRequest, firstUserId),
        () -> reviewService.create(secondRequest, secondUserId));

    assertContentStatistics(2, 4.0);
    assertThat(reviewRepository.countByTargetContentId(contentId)).isEqualTo(2L);
  }

  @Test
  @DisplayName("동일 콘텐츠의 리뷰 수정과 삭제가 동시에 실행돼도 최종 통계가 일치한다")
  void concurrentUpdateAndDelete_updatesStatisticsCorrectly() throws Exception {
    ReviewDto firstReview =
        reviewService.create(new ReviewCreateRequest(contentId, "수정할 리뷰", 1), firstUserId);

    ReviewDto secondReview =
        reviewService.create(new ReviewCreateRequest(contentId, "삭제할 리뷰", 3), secondUserId);

    runConcurrently(
        () ->
            reviewService.update(
                firstReview.id(), new ReviewUpdateRequest("수정된 리뷰", 5), firstUserId),
        () -> {
          reviewService.delete(secondReview.id(), secondUserId);
          return null;
        });

    assertContentStatistics(1, 5.0);
    assertThat(reviewRepository.countByTargetContentId(contentId)).isEqualTo(1L);
  }

  @Test
  @DisplayName("동일 콘텐츠의 마지막 리뷰들을 동시에 삭제하면 통계가 0으로 초기화된다")
  void concurrentDeleteLastReviews_resetsStatisticsToZero() throws Exception {
    ReviewDto firstReview =
        reviewService.create(new ReviewCreateRequest(contentId, "첫 번째 삭제 리뷰", 5), firstUserId);

    ReviewDto secondReview =
        reviewService.create(new ReviewCreateRequest(contentId, "두 번째 삭제 리뷰", 3), secondUserId);

    runConcurrently(
        () -> {
          reviewService.delete(firstReview.id(), firstUserId);
          return null;
        },
        () -> {
          reviewService.delete(secondReview.id(), secondUserId);
          return null;
        });

    assertContentStatistics(0, 0.0);
    assertThat(reviewRepository.countByTargetContentId(contentId)).isZero();
  }

  private void assertContentStatistics(int expectedReviewCount, double expectedAverageRating) {
    transactionTemplate.executeWithoutResult(
        status -> {
          Content content = contentRepository.findById(contentId).orElseThrow();

          assertThat(content.getReviewCount()).isEqualTo(expectedReviewCount);
          assertThat(content.getAverageRating()).isEqualTo(expectedAverageRating);
        });
  }

  private <T> List<T> runConcurrently(Callable<T> firstTask, Callable<T> secondTask)
      throws Exception {
    ExecutorService executor = Executors.newFixedThreadPool(2);
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);

    try {
      Future<T> firstFuture = executor.submit(awaitStart(firstTask, ready, start));
      Future<T> secondFuture = executor.submit(awaitStart(secondTask, ready, start));

      assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
      start.countDown();

      T firstResult = firstFuture.get(10, TimeUnit.SECONDS);
      T secondResult = secondFuture.get(10, TimeUnit.SECONDS);

      return Arrays.asList(firstResult, secondResult);
    } finally {
      executor.shutdownNow();
      executor.awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  private <T> Callable<T> awaitStart(Callable<T> task, CountDownLatch ready, CountDownLatch start) {
    return () -> {
      ready.countDown();
      start.await();
      return task.call();
    };
  }
}
