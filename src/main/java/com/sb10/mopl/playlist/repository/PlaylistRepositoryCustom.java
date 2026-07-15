package com.sb10.mopl.playlist.repository;

import com.sb10.mopl.common.pagination.SortDirection;
import com.sb10.mopl.playlist.entity.Playlist;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface PlaylistRepositoryCustom {

  List<Playlist> findAllByCondition(
      String keywordLike,
      UUID ownerId,
      UUID subscriberId,
      Instant updatedAtCursor,
      Long subscriberCountCursor,
      UUID idAfter,
      String sortBy,
      SortDirection sortDirection,
      Pageable pageable);

  long countByCondition(String keywordLike, UUID ownerId, UUID subscriberId);

  Optional<Playlist> findByIdWithOwner(UUID playlistId);
}
