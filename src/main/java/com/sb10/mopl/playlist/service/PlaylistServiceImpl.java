package com.sb10.mopl.playlist.service;

import com.sb10.mopl.common.pagination.CursorPageResponse;
import com.sb10.mopl.common.pagination.SortDirection;
import com.sb10.mopl.playlist.dto.PlaylistCreateRequest;
import com.sb10.mopl.playlist.dto.PlaylistDto;
import com.sb10.mopl.playlist.entity.Playlist;
import com.sb10.mopl.playlist.exception.PlaylistErrorCode;
import com.sb10.mopl.playlist.exception.PlaylistException;
import com.sb10.mopl.playlist.mapper.PlaylistMapper;
import com.sb10.mopl.playlist.repository.PlaylistRepository;
import com.sb10.mopl.user.entity.User;
import com.sb10.mopl.user.exception.UserErrorCode;
import com.sb10.mopl.user.exception.UserException;
import com.sb10.mopl.user.repository.UserRepository;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaylistServiceImpl implements PlaylistService {

  private final PlaylistRepository playlistRepository;
  private final UserRepository userRepository;
  private final PlaylistMapper playlistMapper;

  // 플레이리스트 목록 조회 최대 limit
  private static final int MAX_PLAYLIST_PAGE_LIMIT = 100;

  @Override
  @Transactional
  public PlaylistDto create(PlaylistCreateRequest request, UUID ownerId) {
    // 플레이리스트 소유자 조회
    User owner =
        userRepository
            .findById(ownerId)
            .orElseThrow(
                () -> new UserException(UserErrorCode.USER_NOT_FOUND, Map.of("ownerId", ownerId)));

    // 요청 DTO를 엔티티로 변환
    Playlist playlist = playlistMapper.toEntity(owner, request);

    // 저장 후 응답 DTO로 변환
    Playlist savedPlaylist = playlistRepository.save(playlist);

    return playlistMapper.toDto(savedPlaylist);
  }

  @Override
  public CursorPageResponse<PlaylistDto> findAll(
      String keywordLike,
      UUID ownerId,
      UUID subscriberId,
      String cursor,
      UUID idAfter,
      Integer limit,
      String sortBy,
      SortDirection sortDirection) {

    // 목록 조회 요청 파라미터 검증
    validateFindAllRequest(cursor, idAfter, limit, sortBy, sortDirection);

    // TODO(#50, #53): 플레이리스트 구독 도메인 구현 후 subscriberIdEqual 필터 지원
    if (subscriberId != null) {
      throw new PlaylistException(
          PlaylistErrorCode.INVALID_PLAYLIST_VALUE,
          Map.of("subscriberId", "구독자 기준 플레이리스트 조회는 아직 지원하지 않습니다."));
    }

    // TODO(#50): 플레이리스트 구독 도메인 구현 후 subscriberCount 정렬 지원
    if ("subscriberCount".equals(sortBy)) {
      throw new PlaylistException(
          PlaylistErrorCode.INVALID_PLAYLIST_VALUE,
          Map.of("sortBy", "subscriberCount 정렬은 아직 지원하지 않습니다."));
    }

    // updatedAt 커서 파싱
    Instant parsedCursor = parseCursor(cursor);

    // 다음 페이지 여부 확인을 위해 limit보다 1개 더 조회
    List<Playlist> playlists =
        playlistRepository.findAllByUpdatedAtCursorDesc(
            normalizeKeyword(keywordLike),
            ownerId,
            parsedCursor,
            idAfter,
            PageRequest.of(0, limit + 1));

    // 조회 결과를 커서 페이지 응답으로 변환
    return toCursorPageResponse(playlists, keywordLike, ownerId, limit, sortBy, sortDirection);
  }

  @Override
  public PlaylistDto findById(UUID playlistId) {
    // 플레이 리스트 존재 검증
    Playlist playlist =
        playlistRepository
            .findByIdWithOwner(playlistId)
            .orElseThrow(
                () ->
                    new PlaylistException(
                        PlaylistErrorCode.PLAYLIST_NOT_FOUND, Map.of("playlistId", playlistId)));

    return playlistMapper.toDto(playlist);
  }

  private void validateFindAllRequest(
      String cursor, UUID idAfter, Integer limit, String sortBy, SortDirection sortDirection) {

    // 커서와 idAfter는 함께 전달
    boolean hasCursor = cursor != null && !cursor.isBlank();
    boolean hasIdAfter = idAfter != null;

    if (hasCursor != hasIdAfter) {
      throw new PlaylistException(
          PlaylistErrorCode.INVALID_PLAYLIST_VALUE,
          Map.of("cursor", "cursor와 idAfter는 함께 전달되어야 합니다."));
    }

    // 정렬 기준 검증
    if (sortBy == null || sortBy.isBlank()) {
      throw new PlaylistException(
          PlaylistErrorCode.INVALID_PLAYLIST_VALUE, Map.of("sortBy", "정렬 기준은 필수입니다."));
    }

    // 정렬 기준 값 검증
    if (!"updatedAt".equals(sortBy) && !"subscriberCount".equals(sortBy)) {
      throw new PlaylistException(
          PlaylistErrorCode.INVALID_PLAYLIST_VALUE, Map.of("sortBy", "지원하지 않는 정렬 기준입니다."));
    }

    // 정렬 방향 값 검증
    if (sortDirection != SortDirection.DESCENDING) {
      throw new PlaylistException(
          PlaylistErrorCode.INVALID_PLAYLIST_VALUE,
          Map.of("sortDirection", "현재는 DESCENDING 정렬만 지원합니다."));
    }

    // 요청 limit이 유효한지 검증
    if (limit == null || limit <= 0 || limit > MAX_PLAYLIST_PAGE_LIMIT) {
      throw new PlaylistException(
          PlaylistErrorCode.INVALID_PLAYLIST_VALUE,
          Map.of("limit", "limit은 1 이상 " + MAX_PLAYLIST_PAGE_LIMIT + " 이하여야 합니다."));
    }
  }

  private Instant parseCursor(String cursor) {
    // 커서가 비어 있으면 첫 페이지 조회로 처리
    if (cursor == null || cursor.isBlank()) {
      return null;
    }

    try {
      // ISO-8601 문자열을 Instant로 변환
      return Instant.parse(cursor);
    } catch (DateTimeParseException e) {
      throw new PlaylistException(
          PlaylistErrorCode.INVALID_PLAYLIST_VALUE, Map.of("cursor", "올바르지 않은 커서 형식입니다."), e);
    }
  }

  private CursorPageResponse<PlaylistDto> toCursorPageResponse(
      List<Playlist> playlists,
      String keywordLike,
      UUID ownerId,
      Integer limit,
      String sortBy,
      SortDirection sortDirection) {

    boolean hasNext = playlists.size() > limit;

    List<Playlist> pagePlaylists = hasNext ? playlists.subList(0, limit) : playlists;

    List<PlaylistDto> data = pagePlaylists.stream().map(playlistMapper::toDto).toList();

    Playlist lastPlaylist =
        hasNext && !pagePlaylists.isEmpty() ? pagePlaylists.get(pagePlaylists.size() - 1) : null;

    long totalCount =
        playlistRepository.countBySearchCondition(normalizeKeyword(keywordLike), ownerId);

    return new CursorPageResponse<>(
        data,
        getNextCursor(lastPlaylist),
        getNextIdAfter(lastPlaylist),
        hasNext,
        totalCount,
        sortBy,
        sortDirection);
  }

  private String normalizeKeyword(String keywordLike) {
    return keywordLike == null || keywordLike.isBlank() ? null : keywordLike.trim();
  }

  private String getNextCursor(Playlist lastPlaylist) {
    // 마지막 플리가 없으면 다음 커서를 생성하지 않음
    return lastPlaylist == null ? null : lastPlaylist.getUpdatedAt().toString();
  }

  private UUID getNextIdAfter(Playlist lastPlaylist) {
    // 마지막 플리가 없으면 다음 보조 커서를 생성하지 않음
    return lastPlaylist == null ? null : lastPlaylist.getId();
  }
}
