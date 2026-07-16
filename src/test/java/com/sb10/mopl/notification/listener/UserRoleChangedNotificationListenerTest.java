package com.sb10.mopl.notification.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.sb10.mopl.notification.dto.NotificationDto;
import com.sb10.mopl.notification.entity.NotificationLevel;
import com.sb10.mopl.sse.service.SseService;
import com.sb10.mopl.user.entity.UserRole;
import com.sb10.mopl.user.event.UserRoleChangedEvent;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Collection;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@ExtendWith(MockitoExtension.class)
class UserRoleChangedNotificationListenerTest {

  private static final String NOTIFICATIONS_EVENT_NAME = "notifications";

  @Mock private SseService sseService;

  @InjectMocks private UserRoleChangedNotificationListener listener;

  @Test
  @DisplayName("권한 변경 이벤트를 notifications SSE 알림으로 발송한다")
  void sendRoleChangedNotification_sendsNotificationSseEvent() {
    // given
    UUID targetUserId = UUID.randomUUID();
    Instant occurredAt = Instant.parse("2026-07-08T00:00:00Z");
    UserRoleChangedEvent event =
        new UserRoleChangedEvent(
            targetUserId, UserRole.USER, UserRole.ADMIN, UUID.randomUUID(), occurredAt);

    // when
    listener.sendRoleChangedNotification(event);

    // then
    ArgumentCaptor<Collection<UUID>> receiverIdsCaptor = collectionCaptor();
    ArgumentCaptor<Object> dataCaptor = ArgumentCaptor.forClass(Object.class);

    verify(sseService)
        .send(receiverIdsCaptor.capture(), eq(NOTIFICATIONS_EVENT_NAME), dataCaptor.capture());

    NotificationDto notification = (NotificationDto) dataCaptor.getValue();
    assertAll(
        () -> assertThat(receiverIdsCaptor.getValue()).containsExactly(targetUserId),
        () -> assertThat(notification.id()).isNotNull(),
        () -> assertThat(notification.createdAt()).isEqualTo(occurredAt),
        () -> assertThat(notification.receiverId()).isEqualTo(targetUserId),
        () -> assertThat(notification.title()).isEqualTo("내 권한이 변경되었어요."),
        () -> assertThat(notification.content()).contains("USER", "ADMIN"),
        () -> assertThat(notification.level()).isEqualTo(NotificationLevel.INFO));
  }

  @Test
  @DisplayName("SSE 발송 실패 시 예외를 전파하지 않는다")
  void sendRoleChangedNotification_doesNotPropagateException_whenSseSendFails() {
    // given
    UserRoleChangedEvent event =
        new UserRoleChangedEvent(
            UUID.randomUUID(),
            UserRole.USER,
            UserRole.ADMIN,
            UUID.randomUUID(),
            Instant.parse("2026-07-08T00:00:00Z"));

    doThrow(new RuntimeException("sse failed"))
        .when(sseService)
        .send(any(), eq(NOTIFICATIONS_EVENT_NAME), any(NotificationDto.class));

    // when & then
    assertDoesNotThrow(() -> listener.sendRoleChangedNotification(event));

    verify(sseService).send(any(), eq(NOTIFICATIONS_EVENT_NAME), any());
  }

  @Test
  @DisplayName("권한 변경 알림 리스너는 커밋 이후 비동기로 처리한다")
  void sendRoleChangedNotification_hasAfterCommitAsyncPolicy() throws Exception {
    // when
    Method method =
        UserRoleChangedNotificationListener.class.getMethod(
            "sendRoleChangedNotification", UserRoleChangedEvent.class);

    TransactionalEventListener eventListener =
        method.getAnnotation(TransactionalEventListener.class);
    Async async = method.getAnnotation(Async.class);

    // then
    assertAll(
        () -> assertThat(eventListener).isNotNull(),
        () -> assertThat(eventListener.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT),
        () -> assertThat(async).isNotNull(),
        () -> assertThat(async.value()).isEqualTo("notificationExecutor"));
  }

  @SuppressWarnings("unchecked")
  private ArgumentCaptor<Collection<UUID>> collectionCaptor() {
    return ArgumentCaptor.forClass(Collection.class);
  }
}
