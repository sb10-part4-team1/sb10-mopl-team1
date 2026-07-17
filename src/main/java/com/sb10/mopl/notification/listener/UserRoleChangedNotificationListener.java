package com.sb10.mopl.notification.listener;

import com.sb10.mopl.notification.entity.NotificationLevel;
import com.sb10.mopl.notification.service.NotificationService;
import com.sb10.mopl.user.event.UserRoleChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserRoleChangedNotificationListener {

  private final NotificationService notificationService;

  @Async("notificationExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void sendRoleChangedNotification(UserRoleChangedEvent event) {
    try {
      notificationService.create(
          event.targetUserId(),
          "내 권한이 변경되었어요.",
          "내 권한이 [" + event.previousRole().name() + "]에서 [" + event.newRole().name() + "]로 변경되었어요.",
          NotificationLevel.INFO);
    } catch (RuntimeException exception) {
      log.error(
          "권한 변경 알림 생성 실패 - targetUserId: {}, previousRole: {}, "
              + "newRole: {}, changedByUserId: {}, exceptionType: {}",
          event.targetUserId(),
          event.previousRole(),
          event.newRole(),
          event.changedByUserId(),
          exception.getClass().getSimpleName(),
          exception);
    }
  }
}
