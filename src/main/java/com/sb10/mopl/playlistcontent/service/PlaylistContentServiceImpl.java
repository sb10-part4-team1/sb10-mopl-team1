package com.sb10.mopl.playlistcontent.service;

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
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaylistContentServiceImpl implements PlaylistContentService {

  private final PlaylistContentRepository playlistContentRepository;
  private final PlaylistRepository playlistRepository;
  private final ContentRepository contentRepository;

  @Override
  @Transactional
  public void add(UUID playlistId, UUID contentId, UUID userId) {
    Playlist playlist = getPlaylist(playlistId);
    validatePlaylistOwner(playlist, userId);

    Content content = getContent(contentId);

    if (playlistContentRepository.existsByPlaylistIdAndContentId(playlistId, contentId)) {
      throw new PlaylistContentException(
          PlaylistContentErrorCode.PLAYLIST_CONTENT_ALREADY_EXISTS,
          Map.of("playlistId", playlistId, "contentId", contentId));
    }

    PlaylistContent playlistContent = new PlaylistContent(playlist, content);
    playlistContentRepository.save(playlistContent);
  }

  @Override
  @Transactional
  public void delete(UUID playlistId, UUID contentId, UUID userId) {
    Playlist playlist = getPlaylist(playlistId);
    validatePlaylistOwner(playlist, userId);

    PlaylistContent playlistContent =
        playlistContentRepository
            .findByPlaylistIdAndContentId(playlistId, contentId)
            .orElseThrow(
                () ->
                    new PlaylistContentException(
                        PlaylistContentErrorCode.PLAYLIST_CONTENT_NOT_FOUND,
                        Map.of("playlistId", playlistId, "contentId", contentId)));

    playlistContentRepository.delete(playlistContent);
  }

  private Playlist getPlaylist(UUID playlistId) {
    return playlistRepository
        .findByIdWithOwner(playlistId)
        .orElseThrow(
            () ->
                new PlaylistException(
                    PlaylistErrorCode.PLAYLIST_NOT_FOUND, Map.of("playlistId", playlistId)));
  }

  private Content getContent(UUID contentId) {
    return contentRepository
        .findById(contentId)
        .orElseThrow(
            () ->
                new ContentException(
                    ContentErrorCode.CONTENT_NOT_FOUND, Map.of("contentId", contentId)));
  }

  private void validatePlaylistOwner(Playlist playlist, UUID userId) {
    UUID ownerId = playlist.getOwner().getId();

    if (!ownerId.equals(userId)) {
      throw new PlaylistContentException(
          PlaylistContentErrorCode.UNAUTHORIZED_PLAYLIST_CONTENT_ACCESS,
          Map.of("playlistId", playlist.getId(), "userId", userId));
    }
  }
}
