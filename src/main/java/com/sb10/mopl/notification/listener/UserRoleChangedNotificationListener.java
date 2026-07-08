package com.sb10.mopl.notification.listener;

import com.sb10.mopl.notification.dto.NotificationDto;
import com.sb10.mopl.notification.dto.NotificationLevel;
import com.sb10.mopl.sse.service.SseService;
import com.sb10.mopl.user.event.UserRoleChangedEvent;
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
public class UserRoleChangedNotificationListener {

  private static final String NOTIFICATIONS_EVENT_NAME = "notifications";

  private final SseService sseService;

  @Async("notificationExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void sendRoleChangedNotification(UserRoleChangedEvent event) {
    // TODO: Notification 엔티티가 구현되면 NotificationService에서 저장 후 DTO로 변환해 발송한다.
    NotificationDto notification =
        new NotificationDto(
            UUID.randomUUID(),
            event.occurredAt(),
            event.targetUserId(),
            "내 권한이 변경되었어요.",
            "내 권한이 ["
                + event.previousRole().name()
                + "]에서 ["
                + event.newRole().name()
                + "]로 변경되었어요.",
            NotificationLevel.INFO);

    try {
      sseService.send(List.of(event.targetUserId()), NOTIFICATIONS_EVENT_NAME, notification);
    } catch (RuntimeException exception) {
      log.error(
          "권한 변경 알림 발송 실패 - targetUserId: {}, previousRole: {}, "
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
