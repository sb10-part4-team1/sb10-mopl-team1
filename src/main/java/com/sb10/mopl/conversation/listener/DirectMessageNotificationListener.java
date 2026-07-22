package com.sb10.mopl.conversation.listener;

import com.sb10.mopl.conversation.dto.DirectMessageDto;
import com.sb10.mopl.conversation.event.DirectMessageSentEvent;
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
public class DirectMessageNotificationListener {

  private final NotificationService notificationService;

  @Async("notificationExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void sendDirectMessageNotification(DirectMessageSentEvent event) {
    DirectMessageDto directMessage = event.directMessage();
    try {
      notificationService.create(
          event.receiverId(),
          "[DM] " + directMessage.sender().name(),
          directMessage.content(),
          NotificationLevel.INFO);
    } catch (RuntimeException exception) {
      log.error(
          "[SSE-NOTIFICATION] DM 수신 알림 생성 실패 -"
              + "receiverId: {}, conversationId: {}, exceptionType: {}",
          event.receiverId(),
          directMessage.conversationId(),
          exception.getClass().getSimpleName(),
          exception);
    }
  }
}
