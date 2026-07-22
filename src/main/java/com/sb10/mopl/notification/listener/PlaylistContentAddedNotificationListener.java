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

  private final NotificationService notificationService;
  private final PlaylistSubscriptionRepository playlistSubscriptionRepository;

  // 콘텐츠가 추가된 시점(커밋 이후)의 구독자 전체에게 알림을 보낸다.
  @Async("notificationExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void sendPlaylistContentAddedNotification(PlaylistContentAddedEvent event) {
    List<UUID> subscriberIds =
        playlistSubscriptionRepository.findSubscriberIdsByPlaylistId(event.playlistId());

    for (UUID subscriberId : subscriberIds) {
      try {
        notificationService.create(
            subscriberId,
            "구독 중인 플레이리스트에 콘텐츠가 추가됐어요.",
            "[" + event.playlistTitle() + "]에 [" + event.contentTitle() + "]가 추가됐어요.",
            NotificationLevel.INFO);
      } catch (RuntimeException exception) {
        log.error(
            "플레이리스트 콘텐츠 추가 알림 생성 실패 - subscriberId: {}, playlistId: {}, exceptionType: {}",
            subscriberId,
            event.playlistId(),
            exception.getClass().getSimpleName(),
            exception);
      }
    }
  }
}
