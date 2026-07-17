package com.sb10.mopl.playlistsubscription.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.sb10.mopl.playlist.entity.Playlist;
import com.sb10.mopl.playlistsubscription.exception.PlaylistSubscriptionErrorCode;
import com.sb10.mopl.playlistsubscription.exception.PlaylistSubscriptionException;
import com.sb10.mopl.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PlaylistSubscriptionTest {

  private User subscriber;
  private Playlist playlist;

  @BeforeEach
  void setUp() {
    subscriber = mock(User.class);
    playlist = mock(Playlist.class);
  }

  @Test
  @DisplayName("구독 요청자와 플레이리스트가 유효하면 PlaylistSubscription을 생성한다")
  void create_success_whenSubscriberAndPlaylistAreValid() {
    // when
    PlaylistSubscription playlistSubscription = new PlaylistSubscription(subscriber, playlist);

    // then
    assertSame(subscriber, playlistSubscription.getSubscriber());
    assertSame(playlist, playlistSubscription.getPlaylist());
  }

  @Test
  @DisplayName("구독 요청자가 null이면 PlaylistSubscription 생성에 실패한다")
  void create_throwInvalidValue_whenSubscriberIsNull() {
    // when
    PlaylistSubscriptionException exception =
        assertThrows(
            PlaylistSubscriptionException.class, () -> new PlaylistSubscription(null, playlist));

    // then
    assertEquals(
        PlaylistSubscriptionErrorCode.INVALID_PLAYLIST_SUBSCRIPTION_VALUE,
        exception.getErrorCode());
    assertTrue(exception.getDetails().containsKey("subscriber"));
  }

  @Test
  @DisplayName("플레이리스트가 null이면 PlaylistSubscription 생성에 실패한다")
  void create_throwInvalidValue_whenPlaylistIsNull() {
    // when
    PlaylistSubscriptionException exception =
        assertThrows(
            PlaylistSubscriptionException.class, () -> new PlaylistSubscription(subscriber, null));

    // then
    assertEquals(
        PlaylistSubscriptionErrorCode.INVALID_PLAYLIST_SUBSCRIPTION_VALUE,
        exception.getErrorCode());
    assertTrue(exception.getDetails().containsKey("playlist"));
  }

  @Test
  @DisplayName("구독 요청자와 플레이리스트가 모두 null이면 두 필드의 검증 정보를 포함한다")
  void create_throwInvalidValue_whenSubscriberAndPlaylistAreNull() {
    // when
    PlaylistSubscriptionException exception =
        assertThrows(
            PlaylistSubscriptionException.class, () -> new PlaylistSubscription(null, null));

    // then
    assertEquals(
        PlaylistSubscriptionErrorCode.INVALID_PLAYLIST_SUBSCRIPTION_VALUE,
        exception.getErrorCode());
    assertTrue(exception.getDetails().containsKey("subscriber"));
    assertTrue(exception.getDetails().containsKey("playlist"));
  }
}
