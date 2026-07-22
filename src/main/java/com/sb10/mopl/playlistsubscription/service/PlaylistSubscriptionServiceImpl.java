package com.sb10.mopl.playlistsubscription.service;

import com.sb10.mopl.playlist.entity.Playlist;
import com.sb10.mopl.playlist.exception.PlaylistErrorCode;
import com.sb10.mopl.playlist.exception.PlaylistException;
import com.sb10.mopl.playlist.repository.PlaylistRepository;
import com.sb10.mopl.playlistsubscription.entity.PlaylistSubscription;
import com.sb10.mopl.playlistsubscription.event.PlaylistSubscribedEvent;
import com.sb10.mopl.playlistsubscription.exception.PlaylistSubscriptionErrorCode;
import com.sb10.mopl.playlistsubscription.exception.PlaylistSubscriptionException;
import com.sb10.mopl.playlistsubscription.repository.PlaylistSubscriptionRepository;
import com.sb10.mopl.user.entity.User;
import com.sb10.mopl.user.exception.UserErrorCode;
import com.sb10.mopl.user.exception.UserException;
import com.sb10.mopl.user.repository.UserRepository;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaylistSubscriptionServiceImpl implements PlaylistSubscriptionService {

  private final UserRepository userRepository;
  private final PlaylistRepository playlistRepository;
  private final PlaylistSubscriptionRepository playlistSubscriptionRepository;
  private final ApplicationEventPublisher eventPublisher;

  @Override
  @Transactional
  public void subscribe(UUID subscriberId, UUID playlistId) {
    User subscriber = getSubscriber(subscriberId);
    Playlist playlist = getPlaylistWithOwner(playlistId);

    validateNotOwnPlaylist(subscriberId, playlist);
    validateSubscriptionNotExists(subscriberId, playlistId);

    PlaylistSubscription playlistSubscription = new PlaylistSubscription(subscriber, playlist);

    playlistSubscriptionRepository.save(playlistSubscription);

    eventPublisher.publishEvent(
        new PlaylistSubscribedEvent(subscriberId, playlistId, playlist.getOwner().getId()));
  }

  @Override
  @Transactional
  public void unsubscribe(UUID subscriberId, UUID playlistId) {
    PlaylistSubscription playlistSubscription =
        getSubscriptionBySubscriberAndPlaylist(subscriberId, playlistId);

    playlistSubscriptionRepository.delete(playlistSubscription);
  }

  // 본인 소유의 플레이리스트는 구독할 수 없도록 검증
  private void validateNotOwnPlaylist(UUID subscriberId, Playlist playlist) {
    if (playlist.getOwner().getId().equals(subscriberId)) {
      throw new PlaylistSubscriptionException(
          PlaylistSubscriptionErrorCode.UNAUTHORIZED_PLAYLIST_SUBSCRIPTION_ACCESS,
          Map.of("subscriberId", subscriberId, "playlistId", playlist.getId()));
    }
  }

  // 구독 요청자 유저 존재 여부 검증 및 조회
  private User getSubscriber(UUID subscriberId) {
    return userRepository
        .findByIdAndIsDeletedFalse(subscriberId)
        .orElseThrow(
            () ->
                new UserException(
                    UserErrorCode.USER_NOT_FOUND, Map.of("subscriberId", subscriberId)));
  }

  // 구독 대상 플레이리스트 존재 여부 검증 및 조회
  private Playlist getPlaylistWithOwner(UUID playlistId) {
    return playlistRepository
        .findByIdWithOwner(playlistId)
        .orElseThrow(
            () ->
                new PlaylistException(
                    PlaylistErrorCode.PLAYLIST_NOT_FOUND, Map.of("playlistId", playlistId)));
  }

  // 이미 구독한 플레이리스트인지 검증
  private void validateSubscriptionNotExists(UUID subscriberId, UUID playlistId) {
    if (playlistSubscriptionRepository.existsBySubscriberIdAndPlaylistId(
        subscriberId, playlistId)) {
      throw new PlaylistSubscriptionException(
          PlaylistSubscriptionErrorCode.PLAYLIST_SUBSCRIPTION_ALREADY_EXISTS,
          Map.of("subscriberId", subscriberId, "playlistId", playlistId));
    }
  }

  // 구독자와 플레이리스트 기준으로 구독 관계 조회
  private PlaylistSubscription getSubscriptionBySubscriberAndPlaylist(
      UUID subscriberId, UUID playlistId) {
    return playlistSubscriptionRepository
        .findBySubscriberIdAndPlaylistId(subscriberId, playlistId)
        .orElseThrow(
            () ->
                new PlaylistSubscriptionException(
                    PlaylistSubscriptionErrorCode.PLAYLIST_SUBSCRIPTION_NOT_FOUND,
                    Map.of("subscriberId", subscriberId, "playlistId", playlistId)));
  }
}
