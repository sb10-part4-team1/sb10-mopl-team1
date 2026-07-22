package com.sb10.mopl.playlistcontent.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sb10.mopl.content.entity.Content;
import com.sb10.mopl.content.exception.ContentErrorCode;
import com.sb10.mopl.content.exception.ContentException;
import com.sb10.mopl.content.repository.ContentRepository;
import com.sb10.mopl.playlist.entity.Playlist;
import com.sb10.mopl.playlist.exception.PlaylistErrorCode;
import com.sb10.mopl.playlist.exception.PlaylistException;
import com.sb10.mopl.playlist.repository.PlaylistRepository;
import com.sb10.mopl.playlistcontent.entity.PlaylistContent;
import com.sb10.mopl.playlistcontent.exception.PlaylistContentErrorCode;
import com.sb10.mopl.playlistcontent.exception.PlaylistContentException;
import com.sb10.mopl.playlistcontent.repository.PlaylistContentRepository;
import com.sb10.mopl.user.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PlaylistContentServiceImplTest {

  @Mock private PlaylistContentRepository playlistContentRepository;

  @Mock private PlaylistRepository playlistRepository;

  @Mock private ContentRepository contentRepository;

  @Mock private ApplicationEventPublisher eventPublisher;

  @InjectMocks private PlaylistContentServiceImpl playlistContentService;

  private UUID playlistId;
  private UUID contentId;
  private UUID ownerId;
  private UUID otherUserId;

  private User owner;
  private User otherUser;
  private Playlist playlist;
  private Playlist otherUserPlaylist;
  private Content content;
  private PlaylistContent playlistContent;

  @BeforeEach
  void setUp() {
    playlistId = UUID.randomUUID();
    contentId = UUID.randomUUID();
    ownerId = UUID.randomUUID();
    otherUserId = UUID.randomUUID();

    owner = createUser(ownerId);
    otherUser = createUser(otherUserId);

    playlist = createPlaylist(playlistId, owner);
    otherUserPlaylist = createPlaylist(playlistId, otherUser);

    content = org.mockito.Mockito.mock(Content.class);
    playlistContent = new PlaylistContent(playlist, content);
  }

  @Test
  @DisplayName("플레이리스트 콘텐츠 추가 요청이 유효하면 콘텐츠를 추가한다")
  void add_success_whenRequestIsValid() {
    // given
    when(playlistRepository.findByIdWithOwner(playlistId)).thenReturn(Optional.of(playlist));
    when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));
    when(playlistContentRepository.existsByPlaylistIdAndContentId(playlistId, contentId))
        .thenReturn(false);

    // when
    assertDoesNotThrow(() -> playlistContentService.add(playlistId, contentId, ownerId));

    // then
    ArgumentCaptor<PlaylistContent> captor = ArgumentCaptor.forClass(PlaylistContent.class);
    verify(playlistContentRepository).save(captor.capture());

    PlaylistContent savedPlaylistContent = captor.getValue();
    assertSame(playlist, savedPlaylistContent.getPlaylist());
    assertSame(content, savedPlaylistContent.getContent());
  }

  @Test
  @DisplayName("플레이리스트 콘텐츠 추가 실패 - 플레이리스트가 없으면 예외를 발생시킨다")
  void add_throwPlaylistNotFound_whenPlaylistDoesNotExist() {
    // given
    when(playlistRepository.findByIdWithOwner(playlistId)).thenReturn(Optional.empty());

    // when
    PlaylistException exception =
        assertThrows(
            PlaylistException.class,
            () -> playlistContentService.add(playlistId, contentId, ownerId));

    // then
    assertEquals(PlaylistErrorCode.PLAYLIST_NOT_FOUND, exception.getErrorCode());
    verify(contentRepository, never()).findById(any(UUID.class));
    verify(playlistContentRepository, never()).save(any(PlaylistContent.class));
  }

  @Test
  @DisplayName("플레이리스트 콘텐츠 추가 실패 - 소유자가 아니면 예외를 발생시킨다")
  void add_throwUnauthorized_whenUserIsNotOwner() {
    // given
    when(playlistRepository.findByIdWithOwner(playlistId))
        .thenReturn(Optional.of(otherUserPlaylist));

    // when
    PlaylistContentException exception =
        assertThrows(
            PlaylistContentException.class,
            () -> playlistContentService.add(playlistId, contentId, ownerId));

    // then
    assertEquals(
        PlaylistContentErrorCode.UNAUTHORIZED_PLAYLIST_CONTENT_ACCESS, exception.getErrorCode());
    verify(contentRepository, never()).findById(any(UUID.class));
    verify(playlistContentRepository, never()).save(any(PlaylistContent.class));
  }

  @Test
  @DisplayName("플레이리스트 콘텐츠 추가 실패 - 콘텐츠가 없으면 예외를 발생시킨다")
  void add_throwContentNotFound_whenContentDoesNotExist() {
    // given
    when(playlistRepository.findByIdWithOwner(playlistId)).thenReturn(Optional.of(playlist));
    when(contentRepository.findById(contentId)).thenReturn(Optional.empty());

    // when
    ContentException exception =
        assertThrows(
            ContentException.class,
            () -> playlistContentService.add(playlistId, contentId, ownerId));

    // then
    assertEquals(ContentErrorCode.CONTENT_NOT_FOUND, exception.getErrorCode());
    verify(playlistContentRepository, never()).save(any(PlaylistContent.class));
  }

  @Test
  @DisplayName("플레이리스트 콘텐츠 추가 실패 - 이미 추가된 콘텐츠이면 예외를 발생시킨다")
  void add_throwAlreadyExists_whenPlaylistContentAlreadyExists() {
    // given
    when(playlistRepository.findByIdWithOwner(playlistId)).thenReturn(Optional.of(playlist));
    when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));
    when(playlistContentRepository.existsByPlaylistIdAndContentId(playlistId, contentId))
        .thenReturn(true);

    // when
    PlaylistContentException exception =
        assertThrows(
            PlaylistContentException.class,
            () -> playlistContentService.add(playlistId, contentId, ownerId));

    // then
    assertEquals(
        PlaylistContentErrorCode.PLAYLIST_CONTENT_ALREADY_EXISTS, exception.getErrorCode());
    verify(playlistContentRepository, never()).save(any(PlaylistContent.class));
  }

  @Test
  @DisplayName("플레이리스트 콘텐츠 삭제 요청이 유효하면 콘텐츠를 삭제한다")
  void delete_success_whenRequestIsValid() {
    // given
    when(playlistRepository.findByIdWithOwner(playlistId)).thenReturn(Optional.of(playlist));
    when(playlistContentRepository.findByPlaylistIdAndContentId(playlistId, contentId))
        .thenReturn(Optional.of(playlistContent));

    // when
    assertDoesNotThrow(() -> playlistContentService.delete(playlistId, contentId, ownerId));

    // then
    verify(playlistContentRepository).delete(playlistContent);
  }

  @Test
  @DisplayName("플레이리스트 콘텐츠 삭제 실패 - 플레이리스트가 없으면 예외를 발생시킨다")
  void delete_throwPlaylistNotFound_whenPlaylistDoesNotExist() {
    // given
    when(playlistRepository.findByIdWithOwner(playlistId)).thenReturn(Optional.empty());

    // when
    PlaylistException exception =
        assertThrows(
            PlaylistException.class,
            () -> playlistContentService.delete(playlistId, contentId, ownerId));

    // then
    assertEquals(PlaylistErrorCode.PLAYLIST_NOT_FOUND, exception.getErrorCode());
    verify(playlistContentRepository, never()).findByPlaylistIdAndContentId(any(), any());
    verify(playlistContentRepository, never()).delete(any(PlaylistContent.class));
  }

  @Test
  @DisplayName("플레이리스트 콘텐츠 삭제 실패 - 소유자가 아니면 예외를 발생시킨다")
  void delete_throwUnauthorized_whenUserIsNotOwner() {
    // given
    when(playlistRepository.findByIdWithOwner(playlistId))
        .thenReturn(Optional.of(otherUserPlaylist));

    // when
    PlaylistContentException exception =
        assertThrows(
            PlaylistContentException.class,
            () -> playlistContentService.delete(playlistId, contentId, ownerId));

    // then
    assertEquals(
        PlaylistContentErrorCode.UNAUTHORIZED_PLAYLIST_CONTENT_ACCESS, exception.getErrorCode());
    verify(playlistContentRepository, never()).findByPlaylistIdAndContentId(any(), any());
    verify(playlistContentRepository, never()).delete(any(PlaylistContent.class));
  }

  @Test
  @DisplayName("플레이리스트 콘텐츠 삭제 실패 - 매핑 정보가 없으면 예외를 발생시킨다")
  void delete_throwNotFound_whenPlaylistContentDoesNotExist() {
    // given
    when(playlistRepository.findByIdWithOwner(playlistId)).thenReturn(Optional.of(playlist));
    when(playlistContentRepository.findByPlaylistIdAndContentId(playlistId, contentId))
        .thenReturn(Optional.empty());

    // when
    PlaylistContentException exception =
        assertThrows(
            PlaylistContentException.class,
            () -> playlistContentService.delete(playlistId, contentId, ownerId));

    // then
    assertEquals(PlaylistContentErrorCode.PLAYLIST_CONTENT_NOT_FOUND, exception.getErrorCode());
    verify(playlistContentRepository, never()).delete(any(PlaylistContent.class));
  }

  private User createUser(UUID userId) {
    User user = User.createUser("테스트유저", "test@example.com", "password", null);
    ReflectionTestUtils.setField(user, "id", userId);
    return user;
  }

  private Playlist createPlaylist(UUID playlistId, User owner) {
    Playlist playlist = new Playlist(owner, "플레이리스트 제목", "플레이리스트 설명");
    ReflectionTestUtils.setField(playlist, "id", playlistId);
    return playlist;
  }
}
