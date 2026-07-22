package com.sb10.mopl.notification.listener;

import com.sb10.mopl.follow.event.FollowCreatedEvent;
import com.sb10.mopl.notification.entity.NotificationLevel;
import com.sb10.mopl.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class FollowCreatedNotificationListener {

  private static final String NOTIFICATION_TITLE = "새로운 팔로워가 생겼어요.";
  private static final String NOTIFICATION_CONTENT = "사용자가 회원님을 팔로우했어요.";

  private final NotificationService notificationService;

  @Async("notificationExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void sendFollowCreatedNotification(FollowCreatedEvent event) {
    try {
      notificationService.create(
          event.followeeId(), NOTIFICATION_TITLE, NOTIFICATION_CONTENT, NotificationLevel.INFO);
    } catch (RuntimeException e) {
      log.error(
          "팔로우 알림 생성 실패 followerId = {}, followeeId = {}, exceptionType = {}",
          event.followerId(),
          event.followeeId(),
          e.getClass().getSimpleName(),
          e);
    }
  }
}
