package com.sb10.mopl.notification.listener;

import com.sb10.mopl.follow.repository.FollowRepository;
import com.sb10.mopl.notification.entity.NotificationLevel;
import com.sb10.mopl.notification.service.NotificationService;
import com.sb10.mopl.playlist.event.PlaylistCreatedEvent;
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
public class PlaylistCreatedNotificationListener {

  private final NotificationService notificationService;
  private final FollowRepository followRepository;

  @Async("notificationExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void sendPlaylistCreatedNotification(PlaylistCreatedEvent event) {
    List<UUID> followerIds = followRepository.findFollowerIdsByFolloweeId(event.ownerId());

    try {
      notificationService.createAll(
          followerIds,
          event.ownerName() + "님이 플레이리스트를 만들었어요.",
          "[" + event.playlistTitle() + "] " + event.playlistDescription(),
          NotificationLevel.INFO);
    } catch (RuntimeException exception) {
      log.error(
          "플레이리스트 생성 알림 생성 실패 - ownerId: {}, followerCount: {}, exceptionType: {}",
          event.ownerId(),
          followerIds.size(),
          exception.getClass().getSimpleName(),
          exception);
    }
  }
}
