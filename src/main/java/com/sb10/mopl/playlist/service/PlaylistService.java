package com.sb10.mopl.playlist.service;

import com.sb10.mopl.common.pagination.CursorPageResponse;
import com.sb10.mopl.common.pagination.SortDirection;
import com.sb10.mopl.playlist.dto.PlaylistCreateRequest;
import com.sb10.mopl.playlist.dto.PlaylistDto;
import java.util.UUID;

public interface PlaylistService {

  PlaylistDto create(PlaylistCreateRequest request, UUID ownerId);

  // 페이지 네이션 목록 조회
  CursorPageResponse<PlaylistDto> findAll(
      String keywordLike,
      UUID ownerId,
      UUID subscriberId,
      String cursor,
      UUID idAfter,
      Integer limit,
      String sortBy,
      SortDirection sortDirection);

  // 단건 조회
  PlaylistDto findById(UUID playlistId);
}
