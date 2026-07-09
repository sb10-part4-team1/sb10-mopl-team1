package com.sb10.mopl.playlist.service;

import com.sb10.mopl.common.pagination.CursorPageResponse;
import com.sb10.mopl.common.pagination.SortDirection;
import com.sb10.mopl.playlist.dto.PlaylistCreateRequest;
import com.sb10.mopl.playlist.dto.PlaylistDto;
import com.sb10.mopl.playlist.dto.PlaylistUpdateRequest;
import com.sb10.mopl.playlist.entity.Playlist;
import com.sb10.mopl.playlist.exception.PlaylistErrorCode;
import com.sb10.mopl.playlist.exception.PlaylistException;
import com.sb10.mopl.playlist.mapper.PlaylistMapper;
import com.sb10.mopl.playlist.repository.PlaylistRepository;
import com.sb10.mopl.playlistsubscription.repository.PlaylistSubscriptionRepository;
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
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaylistServiceImpl implements PlaylistService {

  private final PlaylistRepository playlistRepository;
  private final UserRepository userRepository;
  private final PlaylistMapper playlistMapper;
  private final PlaylistSubscriptionRepository playlistSubscriptionRepository;

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

    return toPlaylistDto(savedPlaylist, null);
  }

  @Override
  @Transactional(readOnly = true)
  public CursorPageResponse<PlaylistDto> findAll(
      String keywordLike,
      UUID ownerId,
      UUID subscriberId,
      UUID currentUserId,
      String cursor,
      UUID idAfter,
      Integer limit,
      String sortBy,
      SortDirection sortDirection) {

    // 목록 조회 요청 파라미터 검증
    validateFindAllRequest(cursor, idAfter, limit, sortBy, sortDirection);

    // 검색어 공백 제거 및 빈 문자열 null 처리
    String normalizedKeyword = normalizeKeyword(keywordLike);

    // 정렬 기준에 따라 커서 파싱
    Instant updatedAtCursor = parseUpdatedAtCursor(cursor, sortBy);
    Long subscriberCountCursor = parseSubscriberCountCursor(cursor, sortBy);

    // 다음 페이지 여부 확인을 위해 limit보다 1개 더 조회
    List<Playlist> playlists =
        getPlaylists(
            normalizedKeyword,
            ownerId,
            subscriberId,
            updatedAtCursor,
            subscriberCountCursor,
            idAfter,
            sortBy,
            PageRequest.of(0, limit + 1));

    // 조회 결과를 커서 페이지 응답으로 변환
    return toCursorPageResponse(
        playlists,
        normalizedKeyword,
        ownerId,
        subscriberId,
        currentUserId,
        limit,
        sortBy,
        sortDirection);
  }

  @Override
  @Transactional(readOnly = true)
  public PlaylistDto findById(UUID playlistId, UUID currentUserId) {
    // 플레이리스트 존재 검증
    Playlist playlist =
        playlistRepository
            .findByIdWithOwner(playlistId)
            .orElseThrow(
                () ->
                    new PlaylistException(
                        PlaylistErrorCode.PLAYLIST_NOT_FOUND, Map.of("playlistId", playlistId)));

    return toPlaylistDto(playlist, currentUserId);
  }

  @Override
  @Transactional
  public PlaylistDto update(UUID playlistId, PlaylistUpdateRequest request, UUID userId) {
    // 플레이리스트 존재 여부와 소유자 권한 검증
    Playlist playlist = getPlaylistOwnedBy(playlistId, userId);

    // 플레이리스트 업데이트
    playlist.update(request.title(), request.description());

    return toPlaylistDto(playlist, null);
  }

  @Override
  @Transactional
  public void delete(UUID playlistId, UUID userId) {
    // 플레이리스트 존재 여부와 소유자 권한 검증
    Playlist playlist = getPlaylistOwnedBy(playlistId, userId);

    // 플레이리스트 삭제
    playlistRepository.delete(playlist);
  }

  // 정렬 기준과 구독자 필터 여부에 따라 플레이리스트 목록 조회
  private List<Playlist> getPlaylists(
      String normalizedKeyword,
      UUID ownerId,
      UUID subscriberId,
      Instant updatedAtCursor,
      Long subscriberCountCursor,
      UUID idAfter,
      String sortBy,
      Pageable pageable) {

    if ("subscriberCount".equals(sortBy)) {
      return getPlaylistsSortedBySubscriberCount(
          normalizedKeyword, ownerId, subscriberId, subscriberCountCursor, idAfter, pageable);
    }

    return getPlaylistsSortedByUpdatedAt(
        normalizedKeyword, ownerId, subscriberId, updatedAtCursor, idAfter, pageable);
  }

  // updatedAt 기준 플레이리스트 조회
  private List<Playlist> getPlaylistsSortedByUpdatedAt(
      String normalizedKeyword,
      UUID ownerId,
      UUID subscriberId,
      Instant updatedAtCursor,
      UUID idAfter,
      Pageable pageable) {

    // subscriberId가 없으면 기존 전체/소유자/검색 조건 조회
    if (subscriberId == null) {
      return playlistRepository.findAllByUpdatedAtCursorDesc(
          normalizedKeyword, ownerId, updatedAtCursor, idAfter, pageable);
    }

    // subscriberId가 있으면 해당 사용자가 구독한 플레이리스트 조회
    return playlistRepository.findSubscribedByUpdatedAtCursorDesc(
        normalizedKeyword, ownerId, subscriberId, updatedAtCursor, idAfter, pageable);
  }

  // subscriberCount 기준 플레이리스트 조회
  private List<Playlist> getPlaylistsSortedBySubscriberCount(
      String normalizedKeyword,
      UUID ownerId,
      UUID subscriberId,
      Long subscriberCountCursor,
      UUID idAfter,
      Pageable pageable) {

    // subscriberId가 없으면 전체/소유자/검색 조건에서 구독자 수 기준 조회
    if (subscriberId == null) {
      return playlistRepository.findAllBySubscriberCountCursorDesc(
          normalizedKeyword, ownerId, subscriberCountCursor, idAfter, pageable);
    }

    // subscriberId가 있으면 해당 사용자가 구독한 플레이리스트를 구독자 수 기준으로 조회
    return playlistRepository.findSubscribedBySubscriberCountCursorDesc(
        normalizedKeyword, ownerId, subscriberId, subscriberCountCursor, idAfter, pageable);
  }

  // 플레이리스트 존재 여부와 소유자 권한을 함께 검증
  private Playlist getPlaylistOwnedBy(UUID playlistId, UUID userId) {
    Playlist playlist =
        playlistRepository
            .findByIdWithOwner(playlistId)
            .orElseThrow(
                () ->
                    new PlaylistException(
                        PlaylistErrorCode.PLAYLIST_NOT_FOUND, Map.of("playlistId", playlistId)));

    validatePlaylistOwner(playlist, userId);

    return playlist;
  }

  // 목록 조회 요청 파라미터 검증
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

    // 정렬 기준 필수값 검증
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

    // 요청 limit 유효성 검증
    if (limit == null || limit <= 0 || limit > MAX_PLAYLIST_PAGE_LIMIT) {
      throw new PlaylistException(
          PlaylistErrorCode.INVALID_PLAYLIST_VALUE,
          Map.of("limit", "limit은 1 이상 " + MAX_PLAYLIST_PAGE_LIMIT + " 이하여야 합니다."));
    }
  }

  // 플레이리스트 생성자 권한 검증
  private void validatePlaylistOwner(Playlist playlist, UUID userId) {
    if (!playlist.getOwner().getId().equals(userId)) {
      throw new PlaylistException(
          PlaylistErrorCode.UNAUTHORIZED_PLAYLIST_ACCESS,
          Map.of("playlistId", playlist.getId(), "userId", userId));
    }
  }

  // updatedAt 정렬일 때 커서 문자열을 Instant로 변환
  private Instant parseUpdatedAtCursor(String cursor, String sortBy) {
    if (!"updatedAt".equals(sortBy)) {
      return null;
    }

    // 커서가 비어 있으면 첫 페이지 조회로 처리
    if (cursor == null || cursor.isBlank()) {
      return null;
    }

    try {
      // ISO-8601 문자열을 Instant로 변환
      return Instant.parse(cursor);
    } catch (DateTimeParseException e) {
      throw new PlaylistException(
          PlaylistErrorCode.INVALID_PLAYLIST_VALUE,
          Map.of("cursor", "올바르지 않은 updatedAt 커서 형식입니다."),
          e);
    }
  }

  // subscriberCount 정렬일 때 커서 문자열을 Long으로 변환
  private Long parseSubscriberCountCursor(String cursor, String sortBy) {
    if (!"subscriberCount".equals(sortBy)) {
      return null;
    }

    // 커서가 비어 있으면 첫 페이지 조회로 처리
    if (cursor == null || cursor.isBlank()) {
      return null;
    }

    try {
      long subscriberCountCursor = Long.parseLong(cursor);

      if (subscriberCountCursor < 0) {
        throw new NumberFormatException(
            "subscriberCount cursor must be greater than or equal to 0");
      }

      return subscriberCountCursor;
    } catch (NumberFormatException e) {
      throw new PlaylistException(
          PlaylistErrorCode.INVALID_PLAYLIST_VALUE,
          Map.of("cursor", "올바르지 않은 subscriberCount 커서 형식입니다."),
          e);
    }
  }

  // 조회 결과를 커서 페이지 응답으로 변환
  private CursorPageResponse<PlaylistDto> toCursorPageResponse(
      List<Playlist> playlists,
      String normalizedKeyword,
      UUID ownerId,
      UUID subscriberId,
      UUID currentUserId,
      Integer limit,
      String sortBy,
      SortDirection sortDirection) {

    // limit보다 많이 조회되었으면 다음 페이지 존재
    boolean hasNext = playlists.size() > limit;

    // 응답 데이터는 요청 limit만큼만 사용
    List<Playlist> pagePlaylists = hasNext ? playlists.subList(0, limit) : playlists;

    // Playlist 엔티티 목록을 응답 DTO 목록으로 변환
    List<PlaylistDto> data =
        pagePlaylists.stream().map(playlist -> toPlaylistDto(playlist, currentUserId)).toList();

    // 다음 커서 생성을 위한 마지막 플레이리스트 추출
    Playlist lastPlaylist =
        hasNext && !pagePlaylists.isEmpty() ? pagePlaylists.get(pagePlaylists.size() - 1) : null;

    // 전체 개수 조회
    long totalCount = countPlaylists(normalizedKeyword, ownerId, subscriberId);

    return new CursorPageResponse<>(
        data,
        getNextCursor(lastPlaylist, sortBy),
        getNextIdAfter(lastPlaylist),
        hasNext,
        totalCount,
        sortBy,
        sortDirection);
  }

  // PlaylistDto에 실제 구독자 수와 구독 여부를 반영
  private PlaylistDto toPlaylistDto(Playlist playlist, UUID currentUserId) {
    long subscriberCount = playlistSubscriptionRepository.countByPlaylistId(playlist.getId());

    boolean subscribedByMe =
        currentUserId != null
            && playlistSubscriptionRepository.existsBySubscriberIdAndPlaylistId(
                currentUserId, playlist.getId());

    return playlistMapper.toDto(playlist, subscriberCount, subscribedByMe);
  }

  // subscriberId가 있으면 구독한 플레이리스트 개수, 없으면 기존 검색 조건 개수 조회
  private long countPlaylists(String normalizedKeyword, UUID ownerId, UUID subscriberId) {
    if (subscriberId == null) {
      return playlistRepository.countBySearchCondition(normalizedKeyword, ownerId);
    }

    return playlistRepository.countSubscribedBySearchCondition(
        normalizedKeyword, ownerId, subscriberId);
  }

  // 검색어 공백 제거 및 빈 문자열 null 처리
  private String normalizeKeyword(String keywordLike) {
    return keywordLike == null || keywordLike.isBlank() ? null : keywordLike.trim();
  }

  // 다음 페이지 요청에 사용할 커서 생성
  private String getNextCursor(Playlist lastPlaylist, String sortBy) {
    // 마지막 플레이리스트가 없으면 다음 커서를 생성하지 않음
    if (lastPlaylist == null) {
      return null;
    }

    if ("subscriberCount".equals(sortBy)) {
      return String.valueOf(playlistSubscriptionRepository.countByPlaylistId(lastPlaylist.getId()));
    }

    return lastPlaylist.getUpdatedAt().toString();
  }

  // 다음 페이지 요청에 사용할 보조 커서 생성
  private UUID getNextIdAfter(Playlist lastPlaylist) {
    // 마지막 플레이리스트가 없으면 다음 보조 커서를 생성하지 않음
    return lastPlaylist == null ? null : lastPlaylist.getId();
  }
}
