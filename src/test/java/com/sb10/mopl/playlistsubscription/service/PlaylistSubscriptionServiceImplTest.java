package com.sb10.mopl.playlistsubscription.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class PlaylistSubscriptionServiceImplTest {

  @Mock private UserRepository userRepository;

  @Mock private PlaylistRepository playlistRepository;

  @Mock private PlaylistSubscriptionRepository playlistSubscriptionRepository;

  @Mock private ApplicationEventPublisher eventPublisher;

  @InjectMocks private PlaylistSubscriptionServiceImpl playlistSubscriptionService;

  @Test
  @DisplayName("플레이리스트 구독 - 성공")
  void subscribe_success() {
    // given
    UUID subscriberId = UUID.randomUUID();
    UUID playlistId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();

    String subscriberName = "구독요청자";
    String playlistTitle = "플레이리스트 제목";

    User subscriber = mock(User.class);
    User owner = mock(User.class);
    Playlist playlist = mock(Playlist.class);

    when(owner.getId()).thenReturn(ownerId);
    when(playlist.getOwner()).thenReturn(owner);
    when(playlist.getId()).thenReturn(playlistId);
    when(playlist.getTitle()).thenReturn(playlistTitle);
    when(subscriber.getName()).thenReturn(subscriberName);
    when(userRepository.findByIdAndIsDeletedFalse(subscriberId))
        .thenReturn(Optional.of(subscriber));
    when(playlistRepository.findByIdWithOwner(playlistId)).thenReturn(Optional.of(playlist));
    when(playlistSubscriptionRepository.existsBySubscriberIdAndPlaylistId(subscriberId, playlistId))
        .thenReturn(false);

    // when
    playlistSubscriptionService.subscribe(subscriberId, playlistId);

    // then
    ArgumentCaptor<PlaylistSubscription> captor =
        ArgumentCaptor.forClass(PlaylistSubscription.class);

    verify(playlistSubscriptionRepository).save(captor.capture());

    PlaylistSubscription savedSubscription = captor.getValue();

    assertThat(savedSubscription.getSubscriber()).isSameAs(subscriber);
    assertThat(savedSubscription.getPlaylist()).isSameAs(playlist);

    ArgumentCaptor<PlaylistSubscribedEvent> eventCaptor =
        ArgumentCaptor.forClass(PlaylistSubscribedEvent.class);
    verify(eventPublisher).publishEvent(eventCaptor.capture());

    PlaylistSubscribedEvent publishedEvent = eventCaptor.getValue();
    assertThat(publishedEvent.ownerId()).isEqualTo(ownerId);
    assertThat(publishedEvent.subscriberName()).isEqualTo(subscriberName);
    assertThat(publishedEvent.playlistId()).isEqualTo(playlistId);
    assertThat(publishedEvent.playlistTitle()).isEqualTo(playlistTitle);
  }

  @Test
  @DisplayName("플레이리스트 구독 - 구독 요청자가 존재하지 않으면 실패")
  void subscribe_subscriberNotFound() {
    // given
    UUID subscriberId = UUID.randomUUID();
    UUID playlistId = UUID.randomUUID();

    when(userRepository.findByIdAndIsDeletedFalse(subscriberId)).thenReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> playlistSubscriptionService.subscribe(subscriberId, playlistId))
        .isInstanceOf(UserException.class)
        .satisfies(
            throwable -> {
              UserException exception = (UserException) throwable;

              assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND);
            });

    verify(playlistRepository, never()).findByIdWithOwner(any());
    verify(playlistSubscriptionRepository, never()).save(any());
  }

  @Test
  @DisplayName("플레이리스트 구독 - 플레이리스트가 존재하지 않으면 실패")
  void subscribe_playlistNotFound() {
    // given
    UUID subscriberId = UUID.randomUUID();
    UUID playlistId = UUID.randomUUID();

    User subscriber = mock(User.class);

    when(userRepository.findByIdAndIsDeletedFalse(subscriberId))
        .thenReturn(Optional.of(subscriber));
    when(playlistRepository.findByIdWithOwner(playlistId)).thenReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> playlistSubscriptionService.subscribe(subscriberId, playlistId))
        .isInstanceOf(PlaylistException.class)
        .satisfies(
            throwable -> {
              PlaylistException exception = (PlaylistException) throwable;

              assertThat(exception.getErrorCode()).isEqualTo(PlaylistErrorCode.PLAYLIST_NOT_FOUND);
            });

    verify(playlistSubscriptionRepository, never()).existsBySubscriberIdAndPlaylistId(any(), any());
    verify(playlistSubscriptionRepository, never()).save(any());
  }

  @Test
  @DisplayName("플레이리스트 구독 - 본인 소유의 플레이리스트를 구독하면 실패")
  void subscribe_ownPlaylist() {
    // given
    UUID subscriberId = UUID.randomUUID();
    UUID playlistId = UUID.randomUUID();

    User subscriber = mock(User.class);
    User owner = mock(User.class);
    Playlist playlist = mock(Playlist.class);

    when(owner.getId()).thenReturn(subscriberId);
    when(playlist.getOwner()).thenReturn(owner);
    when(playlist.getId()).thenReturn(playlistId);
    when(userRepository.findByIdAndIsDeletedFalse(subscriberId))
        .thenReturn(Optional.of(subscriber));
    when(playlistRepository.findByIdWithOwner(playlistId)).thenReturn(Optional.of(playlist));

    // when & then
    assertThatThrownBy(() -> playlistSubscriptionService.subscribe(subscriberId, playlistId))
        .isInstanceOf(PlaylistSubscriptionException.class)
        .satisfies(
            throwable -> {
              PlaylistSubscriptionException exception = (PlaylistSubscriptionException) throwable;

              assertThat(exception.getErrorCode())
                  .isEqualTo(
                      PlaylistSubscriptionErrorCode.UNAUTHORIZED_PLAYLIST_SUBSCRIPTION_ACCESS);
            });

    verify(playlistSubscriptionRepository, never()).existsBySubscriberIdAndPlaylistId(any(), any());
    verify(playlistSubscriptionRepository, never()).save(any());
  }

  @Test
  @DisplayName("플레이리스트 구독 - 이미 구독한 플레이리스트이면 실패")
  void subscribe_alreadyExists() {
    // given
    UUID subscriberId = UUID.randomUUID();
    UUID playlistId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();

    User subscriber = mock(User.class);
    User owner = mock(User.class);
    Playlist playlist = mock(Playlist.class);

    when(owner.getId()).thenReturn(ownerId);
    when(playlist.getOwner()).thenReturn(owner);
    when(userRepository.findByIdAndIsDeletedFalse(subscriberId))
        .thenReturn(Optional.of(subscriber));
    when(playlistRepository.findByIdWithOwner(playlistId)).thenReturn(Optional.of(playlist));
    when(playlistSubscriptionRepository.existsBySubscriberIdAndPlaylistId(subscriberId, playlistId))
        .thenReturn(true);

    // when & then
    assertThatThrownBy(() -> playlistSubscriptionService.subscribe(subscriberId, playlistId))
        .isInstanceOf(PlaylistSubscriptionException.class)
        .satisfies(
            throwable -> {
              PlaylistSubscriptionException exception = (PlaylistSubscriptionException) throwable;

              assertThat(exception.getErrorCode())
                  .isEqualTo(PlaylistSubscriptionErrorCode.PLAYLIST_SUBSCRIPTION_ALREADY_EXISTS);
            });

    verify(playlistSubscriptionRepository, never()).save(any());
  }

  @Test
  @DisplayName("플레이리스트 구독 취소 - 성공")
  void unsubscribe_success() {
    // given
    UUID subscriberId = UUID.randomUUID();
    UUID playlistId = UUID.randomUUID();

    PlaylistSubscription playlistSubscription = mock(PlaylistSubscription.class);

    when(playlistSubscriptionRepository.findBySubscriberIdAndPlaylistId(subscriberId, playlistId))
        .thenReturn(Optional.of(playlistSubscription));

    // when
    playlistSubscriptionService.unsubscribe(subscriberId, playlistId);

    // then
    verify(playlistSubscriptionRepository).delete(playlistSubscription);
  }

  @Test
  @DisplayName("플레이리스트 구독 취소 - 구독 관계가 존재하지 않으면 실패")
  void unsubscribe_subscriptionNotFound() {
    // given
    UUID subscriberId = UUID.randomUUID();
    UUID playlistId = UUID.randomUUID();

    when(playlistSubscriptionRepository.findBySubscriberIdAndPlaylistId(subscriberId, playlistId))
        .thenReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> playlistSubscriptionService.unsubscribe(subscriberId, playlistId))
        .isInstanceOf(PlaylistSubscriptionException.class)
        .satisfies(
            throwable -> {
              PlaylistSubscriptionException exception = (PlaylistSubscriptionException) throwable;

              assertThat(exception.getErrorCode())
                  .isEqualTo(PlaylistSubscriptionErrorCode.PLAYLIST_SUBSCRIPTION_NOT_FOUND);
            });

    verify(playlistSubscriptionRepository, never()).delete(any());
  }
}
