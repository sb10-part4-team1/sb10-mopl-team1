package com.sb10.mopl.playlistcontent.service;

import java.util.UUID;

public interface PlaylistContentService {

  void add(UUID playlistId, UUID contentId, UUID userId);

  void delete(UUID playlistId, UUID contentId, UUID userId);
}
