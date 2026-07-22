package com.sb10.mopl.notification.listener;

import com.sb10.mopl.notification.entity.NotificationLevel;
import com.sb10.mopl.notification.service.NotificationService;
import com.sb10.mopl.playlistcontent.event.PlaylistContentAddedEvent;
import com.sb10.mopl.playlistsubscription.repository.PlaylistSubscriptionRepository;
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
public class PlaylistContentAddedNotificationListener {

  private static final String NOTIFICATION_TITLE = "구독 중인 플레이리스트에 콘텐츠가 추가되었어요.";

  private final PlaylistSubscriptionRepository playlistSubscriptionRepository;
  private final NotificationService notificationService;

  @Async("notificationExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void sendPlaylistContentAddedNotification(PlaylistContentAddedEvent event) {
    List<UUID> subscriberIds =
        playlistSubscriptionRepository.findSubscriberIdsByPlaylistId(event.playlistId());

    for (UUID subscriberId : subscriberIds) {
      try {
        notificationService.create(
            subscriberId,
            NOTIFICATION_TITLE,
            "[" + event.playlistTitle() + "] 플레이리스트에 새로운 콘텐츠가 추가되었어요.",
            NotificationLevel.INFO);
      } catch (RuntimeException exception) {
        log.error(
            "플레이리스트 콘텐츠 추가 알림 생성 실패 - "
                + "playlistId: {}, contentId: {}, ownerId: {}, "
                + "subscriberId: {}, exceptionType: {}",
            event.playlistId(),
            event.contentId(),
            event.ownerId(),
            subscriberId,
            exception.getClass().getSimpleName(),
            exception);
      }
    }
  }
}
