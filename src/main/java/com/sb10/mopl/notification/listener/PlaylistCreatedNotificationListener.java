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

  private static final int BATCH_SIZE = 100;

  private final FollowRepository followRepository;
  private final NotificationService notificationService;

  @Async("notificationExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(PlaylistCreatedEvent event) {
    UUID idAfter = null;

    while (true) {
      List<UUID> followerIds =
          followRepository.findFollowerIdsByFolloweeId(event.ownerId(), idAfter, BATCH_SIZE);

      if (followerIds.isEmpty()) {
        break;
      }

      for (UUID followerId : followerIds) {
        try {
          notificationService.create(
              followerId, "새로운 플레이리스트가 등록되었어요.", event.playlistTitle(), NotificationLevel.INFO);
        } catch (RuntimeException exception) {
          log.error(
              "플레이리스트 생성 알림 생성 실패 - ownerId: {}, followerId: {}, playlistTitle: {}",
              event.ownerId(),
              followerId,
              event.playlistTitle(),
              exception);
        }
      }

      if (followerIds.size() < BATCH_SIZE) {
        break;
      }

      idAfter = followerIds.get(followerIds.size() - 1);
    }
  }
}
