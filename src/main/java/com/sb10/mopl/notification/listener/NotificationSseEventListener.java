package com.sb10.mopl.notification.listener;

import com.sb10.mopl.notification.dto.NotificationDto;
import com.sb10.mopl.notification.event.NotificationCreatedEvent;
import com.sb10.mopl.sse.service.SseService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationSseEventListener {

  private static final String NOTIFICATIONS_EVENT_NAME = "notifications";

  private final SseService sseService;

  // 알림 저장 트랜잭션이 커밋된 이후 별도 스레드에서 SSE로 발송
  @Async("notificationExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void sendNotification(NotificationCreatedEvent event) {
    NotificationDto dto = event.notification();
    try {
      sseService.send(List.of(dto.receiverId()), NOTIFICATIONS_EVENT_NAME, dto);
    } catch (RuntimeException exception) {
      log.error(
        "[SSE-NOTIFICATION] 알림 SSE 발송 실패 - receiverId: {}, notificationId: {}, exceptionType: {}",
        dto.receiverId(),
        dto.id(),
        exception.getClass().getSimpleName(),
        exception);
    }
  }
}
