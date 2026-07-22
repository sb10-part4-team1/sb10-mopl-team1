package com.sb10.mopl.notification.listener;

import com.sb10.mopl.notification.entity.NotificationLevel;
import com.sb10.mopl.notification.service.NotificationService;
import com.sb10.mopl.playlistsubscription.event.PlaylistSubscribedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlaylistSubscribedNotificationListener {

  private static final String NOTIFICATION_TITLE = "플레이리스트에 새로운 구독자가 생겼어요.";

  private static final String NOTIFICATION_CONTENT = "사용자가 회원님의 플레이리스트를 구독했어요.";

  private final NotificationService notificationService;

  @Async("notificationExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void sendPlaylistSubscribedNotification(PlaylistSubscribedEvent event) {
    try {
      notificationService.create(
          event.ownerId(), NOTIFICATION_TITLE, NOTIFICATION_CONTENT, NotificationLevel.INFO);
    } catch (RuntimeException exception) {
      log.error(
          "플레이리스트 구독 알림 생성 실패 - "
              + "subscriberId: {}, playlistId: {}, ownerId: {}, exceptionType: {}",
          event.subscriberId(),
          event.playlistId(),
          event.ownerId(),
          exception.getClass().getSimpleName(),
          exception);
    }
  }
}
