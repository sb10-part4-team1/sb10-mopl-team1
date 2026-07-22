package com.sb10.mopl.notification.listener;

import com.sb10.mopl.follow.repository.FollowRepository;
import com.sb10.mopl.notification.entity.NotificationLevel;
import com.sb10.mopl.notification.service.NotificationService;
import com.sb10.mopl.review.event.ReviewCreatedEvent;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewCreatedNotificationListener {

  private static final int BATCH_SIZE = 100;

  private static final String NOTIFICATION_TITLE = "팔로우한 사용자가 새로운 리뷰를 작성했어요.";

  private static final String NOTIFICATION_CONTENT = "팔로우한 사용자가 새로운 리뷰를 작성했어요.";

  private final FollowRepository followRepository;
  private final NotificationService notificationService;

  @Async("notificationExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void sendReviewCreatedNotification(ReviewCreatedEvent event) {
    UUID idAfter = null;

    while (true) {
      List<UUID> followerIds =
          followRepository.findFollowerIdsByFolloweeId(event.reviewerId(), idAfter, BATCH_SIZE);

      if (followerIds.isEmpty()) {
        break;
      }

      createNotifications(event, followerIds);

      if (followerIds.size() < BATCH_SIZE) {
        break;
      }

      idAfter = followerIds.get(followerIds.size() - 1);
    }
  }

  private void createNotifications(ReviewCreatedEvent event, List<UUID> followerIds) {

    for (UUID followerId : followerIds) {
      try {
        notificationService.create(
            followerId, NOTIFICATION_TITLE, NOTIFICATION_CONTENT, NotificationLevel.INFO);
      } catch (RuntimeException exception) {
        log.error(
            "리뷰 작성 알림 생성 실패 - reviewId: {}, reviewerId: {}, "
                + "contentId: {}, followerId: {}, exceptionType: {}",
            event.reviewId(),
            event.reviewerId(),
            event.contentId(),
            followerId,
            exception.getClass().getSimpleName(),
            exception);
      }
    }
  }
}
