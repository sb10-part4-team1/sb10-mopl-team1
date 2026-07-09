package com.sb10.mopl.conversation.listener;

import com.sb10.mopl.conversation.event.DirectMessageSentEvent;
import com.sb10.mopl.sse.service.SseService;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class DirectMessageEventListener {

  private static final String DIRECT_MESSAGE_EVENT_NAME = "direct-messages";

  private final SseService sseService;

  // 비활성 대화 SSE 알림
  @Async("notificationExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void sendDirectMessageNotification(DirectMessageSentEvent event) {
    try {
      sseService.send(Set.of(event.receiverId()), DIRECT_MESSAGE_EVENT_NAME, event.directMessage());
    } catch (RuntimeException exception) {
      log.error(
          "DM SSE 알림 발송 실패 - receiverId: {}, conversationId: {}, exceptionType: {}",
          event.receiverId(),
          event.directMessage().conversationId(),
          exception.getClass().getSimpleName(),
          exception);
    }
  }
}
