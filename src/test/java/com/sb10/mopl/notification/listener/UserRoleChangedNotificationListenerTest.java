package com.sb10.mopl.notification.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.sb10.mopl.notification.entity.NotificationLevel;
import com.sb10.mopl.notification.service.NotificationService;
import com.sb10.mopl.user.entity.UserRole;
import com.sb10.mopl.user.event.UserRoleChangedEvent;
import java.lang.reflect.Method;
import java.time.Instant;
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

  @Mock private NotificationService notificationService;

  @InjectMocks private UserRoleChangedNotificationListener listener;

  @Test
  @DisplayName("권한 변경 이벤트를 받으면 알림을 생성한다")
  void sendRoleChangedNotification_createsNotification() {
    // given
    UUID targetUserId = UUID.randomUUID();
    UserRoleChangedEvent event =
        new UserRoleChangedEvent(
            targetUserId,
            UserRole.USER,
            UserRole.ADMIN,
            UUID.randomUUID(),
            Instant.parse("2026-07-08T00:00:00Z"));

    // when
    listener.sendRoleChangedNotification(event);

    // then
    ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
    verify(notificationService)
        .create(
            eq(targetUserId),
            eq("내 권한이 변경되었어요."),
            contentCaptor.capture(),
            eq(NotificationLevel.INFO));

    assertThat(contentCaptor.getValue()).contains("USER", "ADMIN");
  }

  @Test
  @DisplayName("알림 생성 실패 시 예외를 전파하지 않는다")
  void sendRoleChangedNotification_doesNotPropagateException_whenNotificationCreateFails() {
    // given
    UserRoleChangedEvent event =
        new UserRoleChangedEvent(
            UUID.randomUUID(),
            UserRole.USER,
            UserRole.ADMIN,
            UUID.randomUUID(),
            Instant.parse("2026-07-08T00:00:00Z"));

    doThrow(new RuntimeException("notification create failed"))
        .when(notificationService)
        .create(any(), anyString(), anyString(), eq(NotificationLevel.INFO));

    // when & then
    assertDoesNotThrow(() -> listener.sendRoleChangedNotification(event));

    verify(notificationService).create(any(), anyString(), anyString(), eq(NotificationLevel.INFO));
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
}
