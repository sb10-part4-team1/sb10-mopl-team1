package com.sb10.mopl.watchingsession.service;

import com.sb10.mopl.common.pagination.CursorPageResponse;
import com.sb10.mopl.content.entity.Content;
import com.sb10.mopl.content.exception.ContentErrorCode;
import com.sb10.mopl.content.exception.ContentException;
import com.sb10.mopl.content.repository.ContentRepository;
import com.sb10.mopl.watchingsession.dto.WatchingSessionDto;
import com.sb10.mopl.watchingsession.dto.WatchingSessionJoinResult;
import com.sb10.mopl.watchingsession.dto.WatchingSessionSearchRequest;
import com.sb10.mopl.watchingsession.entity.WatchingSession;
import com.sb10.mopl.watchingsession.mapper.WatchingSessionMapper;
import com.sb10.mopl.watchingsession.repository.WatchingSessionRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class WatchingSessionService {

  private final WatchingSessionRepository watchingSessionRepository;
  private final ContentRepository contentRepository;
  private final WatchingSessionMapper watchingSessionMapper;
  private final WatchingSessionCreator watchingSessionCreator;

  /**
   * 콘텐츠 시청 참여 (SUBSCRIBE /sub/contents/{contentId}/watch 시 호출)
   *
   * <p>유저는 동시에 하나의 콘텐츠만 시청할 수 있다.
   */
  public WatchingSessionJoinResult join(UUID watcherId, UUID contentId) {
    Optional<WatchingSession> existing = watchingSessionRepository.findByWatcherId(watcherId);
    if (existing.isPresent()) {
      return moveTo(existing.get(), contentId);
    }

    // 같은 유저가 동시에 두 탭에서 처음 구독하면 둘 다 여기서 "세션 없음"을 볼 수 있다. 생성은 별도
    // 트랜잭션(WatchingSessionCreator)에 격리해, 유니크 제약을 위반해도 이 트랜잭션은 영향받지 않고
    // 승자의 행을 다시 조회해 이동 로직으로 이어갈 수 있게 한다.
    try {
      WatchingSessionDto created = watchingSessionCreator.create(watcherId, contentId);
      return new WatchingSessionJoinResult(created, null);
    } catch (DataIntegrityViolationException e) {
      WatchingSession winner =
          watchingSessionRepository.findByWatcherId(watcherId).orElseThrow(() -> e);
      return moveTo(winner, contentId);
    }
  }

  // 같은 콘텐츠에 이미 참여 중이면 기존 세션을 그대로 반환하고, 다른 콘텐츠를 보고 있었다면 세션을 이동시킨다.
  private WatchingSessionJoinResult moveTo(WatchingSession watchingSession, UUID contentId) {
    UUID previousContentId = watchingSession.getContent().getId();

    if (previousContentId.equals(contentId)) {
      return new WatchingSessionJoinResult(watchingSessionMapper.toDto(watchingSession), null);
    }

    WatchingSessionDto previousDto = watchingSessionMapper.toDto(watchingSession);
    moveToContent(watchingSession, previousContentId, contentId);

    return new WatchingSessionJoinResult(watchingSessionMapper.toDto(watchingSession), previousDto);
  }

  private void moveToContent(
      WatchingSession watchingSession, UUID previousContentId, UUID contentId) {
    watchingSession.updateContent(findContentOrThrow(contentId));
    contentRepository.decrementWatcherCount(previousContentId);
    contentRepository.incrementWatcherCount(contentId);
  }

  private Content findContentOrThrow(UUID contentId) {
    return contentRepository
        .findById(contentId)
        .orElseThrow(
            () ->
                new ContentException(
                    ContentErrorCode.CONTENT_NOT_FOUND, Map.of("contentId", contentId)));
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
