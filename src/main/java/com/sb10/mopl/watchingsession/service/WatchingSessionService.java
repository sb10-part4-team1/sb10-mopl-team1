package com.sb10.mopl.watchingsession.service;

import com.sb10.mopl.common.pagination.CursorPageResponse;
import com.sb10.mopl.content.entity.Content;
import com.sb10.mopl.content.exception.ContentErrorCode;
import com.sb10.mopl.content.exception.ContentException;
import com.sb10.mopl.content.repository.ContentRepository;
import com.sb10.mopl.user.entity.User;
import com.sb10.mopl.user.exception.UserErrorCode;
import com.sb10.mopl.user.exception.UserException;
import com.sb10.mopl.user.repository.UserRepository;
import com.sb10.mopl.watchingsession.dto.WatchingSessionDto;
import com.sb10.mopl.watchingsession.dto.WatchingSessionSearchRequest;
import com.sb10.mopl.watchingsession.entity.WatchingSession;
import com.sb10.mopl.watchingsession.mapper.WatchingSessionMapper;
import com.sb10.mopl.watchingsession.repository.WatchingSessionRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class WatchingSessionService {

  private final WatchingSessionRepository watchingSessionRepository;
  private final UserRepository userRepository;
  private final ContentRepository contentRepository;
  private final WatchingSessionMapper watchingSessionMapper;

  // 콘텐츠 시청 참여 (SUBSCRIBE /sub/contents/{contentId}/watch 시 호출)
  // 이미 참여 중이면 기존 세션을 그대로 반환합니다 (다중 탭/재구독에 대한 멱등성 보장).
  public WatchingSessionDto join(UUID watcherId, UUID contentId) {
    Optional<WatchingSession> existing =
        watchingSessionRepository.findByWatcherIdAndContentId(watcherId, contentId);
    if (existing.isPresent()) {
      return watchingSessionMapper.toDto(existing.get());
    }

    User watcher =
        userRepository
            .findById(watcherId)
            .orElseThrow(
                () -> new UserException(UserErrorCode.USER_NOT_FOUND, Map.of("userId", watcherId)));

    Content content =
        contentRepository
            .findById(contentId)
            .orElseThrow(
                () ->
                    new ContentException(
                        ContentErrorCode.CONTENT_NOT_FOUND, Map.of("contentId", contentId)));

    WatchingSession watchingSession =
        WatchingSession.builder().watcher(watcher).content(content).build();
    watchingSessionRepository.save(watchingSession);
    contentRepository.incrementWatcherCount(contentId);

    return watchingSessionMapper.toDto(watchingSession);
  }

  // 콘텐츠 시청 이탈 (UNSUBSCRIBE 또는 연결 종료 시 호출). 세션이 없으면 아무 것도 하지 않습니다.
  public Optional<WatchingSessionDto> leave(UUID watcherId, UUID contentId) {
    return watchingSessionRepository
        .findByWatcherIdAndContentId(watcherId, contentId)
        .map(
            watchingSession -> {
              WatchingSessionDto dto = watchingSessionMapper.toDto(watchingSession);
              watchingSessionRepository.delete(watchingSession);
              contentRepository.decrementWatcherCount(contentId);
              return dto;
            });
  }

  // 특정 콘텐츠를 현재 시청 중인 인원 수.
  @Transactional(readOnly = true)
  public long countWatchers(UUID contentId) {
    return contentRepository
        .findById(contentId)
        .map(Content::getWatcherCount)
        .orElseThrow(
            () ->
                new ContentException(
                    ContentErrorCode.CONTENT_NOT_FOUND, Map.of("contentId", contentId)));
  }

  // 특정 사용자의 가장 최근 시청 세션 조회 (nullable)
  @Transactional(readOnly = true)
  public Optional<WatchingSessionDto> findLatestByWatcher(UUID watcherId) {
    return watchingSessionRepository
        .findTopByWatcherIdOrderByCreatedAtDesc(watcherId)
        .map(watchingSessionMapper::toDto);
  }

  // 특정 콘텐츠의 시청 세션 목록 조회 (커서 페이지네이션)
  // 콘텐츠 존재 여부는 검사하지 않는다 (항상 200, 없으면 빈 목록을 반환).
  @Transactional(readOnly = true)
  public CursorPageResponse<WatchingSessionDto> findByContent(
      UUID contentId, WatchingSessionSearchRequest request) {
    List<WatchingSession> result = watchingSessionRepository.search(contentId, request);

    boolean hasNext = result.size() > request.limit();
    List<WatchingSession> data = hasNext ? result.subList(0, request.limit()) : result;

    List<WatchingSessionDto> dtos = data.stream().map(watchingSessionMapper::toDto).toList();

    String nextCursor = null;
    UUID nextIdAfter = null;
    if (hasNext && !data.isEmpty()) {
      WatchingSession last = data.get(data.size() - 1);
      nextCursor = last.getCreatedAt().toString();
      nextIdAfter = last.getId();
    }

    long totalCount = watchingSessionRepository.countByContentId(contentId, request);

    return new CursorPageResponse<>(
        dtos,
        nextCursor,
        nextIdAfter,
        hasNext,
        totalCount,
        request.sortBy().name(),
        request.sortDirection());
  }
}
