package com.sb10.mopl.notification.listener;

import com.sb10.mopl.follow.event.FollowedEvent;
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
public class FollowedNotificationListener {

  private final NotificationService notificationService;

  @Async("notificationExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void sendFollowedNotification(FollowedEvent event) {
    try {
      notificationService.create(
          event.followeeId(), event.followerName() + "님이 나를 팔로우했어요.", "", NotificationLevel.INFO);
    } catch (RuntimeException exception) {
      log.error(
          "팔로우 알림 생성 실패 - followeeId: {}, exceptionType: {}",
          event.followeeId(),
          exception.getClass().getSimpleName(),
          exception);
    }
  }
}
