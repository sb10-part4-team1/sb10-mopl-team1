package com.sb10.mopl.playlistcontent.repository;

import com.sb10.mopl.playlistcontent.entity.PlaylistContent;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlaylistContentRepositoryCustom {

  boolean existsByPlaylistIdAndContentId(UUID playlistId, UUID contentId);

  Optional<PlaylistContent> findByPlaylistIdAndContentId(UUID playlistId, UUID contentId);

  List<PlaylistContent> findAllWithContentByPlaylistIds(List<UUID> playlistIds);
}
