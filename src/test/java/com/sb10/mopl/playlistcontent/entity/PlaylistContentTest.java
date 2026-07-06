package com.sb10.mopl.playlistcontent.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.sb10.mopl.content.entity.Content;
import com.sb10.mopl.playlist.entity.Playlist;
import com.sb10.mopl.playlistcontent.exception.PlaylistContentErrorCode;
import com.sb10.mopl.playlistcontent.exception.PlaylistContentException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PlaylistContentTest {

  private Playlist playlist;
  private Content content;

  @BeforeEach
  void setUp() {
    playlist = mock(Playlist.class);
    content = mock(Content.class);
  }

  @Test
  @DisplayName("플레이리스트와 콘텐츠가 유효하면 PlaylistContent를 생성한다")
  void create_success_whenPlaylistAndContentAreValid() {
    // when
    PlaylistContent playlistContent = new PlaylistContent(playlist, content);

    // then
    assertSame(playlist, playlistContent.getPlaylist());
    assertSame(content, playlistContent.getContent());
  }

  @Test
  @DisplayName("플레이리스트가 null이면 PlaylistContent 생성에 실패한다")
  void create_throwInvalidValue_whenPlaylistIsNull() {
    // when
    PlaylistContentException exception =
        assertThrows(PlaylistContentException.class, () -> new PlaylistContent(null, content));

    // then
    assertEquals(PlaylistContentErrorCode.INVALID_PLAYLIST_CONTENT_VALUE, exception.getErrorCode());
    assertTrue(exception.getDetails().containsKey("playlist"));
  }

  @Test
  @DisplayName("콘텐츠가 null이면 PlaylistContent 생성에 실패한다")
  void create_throwInvalidValue_whenContentIsNull() {
    // when
    PlaylistContentException exception =
        assertThrows(PlaylistContentException.class, () -> new PlaylistContent(playlist, null));

    // then
    assertEquals(PlaylistContentErrorCode.INVALID_PLAYLIST_CONTENT_VALUE, exception.getErrorCode());
    assertTrue(exception.getDetails().containsKey("content"));
  }
}
