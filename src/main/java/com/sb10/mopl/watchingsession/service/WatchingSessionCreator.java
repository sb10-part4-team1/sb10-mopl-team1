package com.sb10.mopl.watchingsession.service;

import com.sb10.mopl.content.entity.Content;
import com.sb10.mopl.content.exception.ContentErrorCode;
import com.sb10.mopl.content.exception.ContentException;
import com.sb10.mopl.content.repository.ContentRepository;
import com.sb10.mopl.user.entity.User;
import com.sb10.mopl.user.exception.UserErrorCode;
import com.sb10.mopl.user.exception.UserException;
import com.sb10.mopl.user.repository.UserRepository;
import com.sb10.mopl.watchingsession.dto.WatchingSessionDto;
import com.sb10.mopl.watchingsession.entity.WatchingSession;
import com.sb10.mopl.watchingsession.mapper.WatchingSessionMapper;
import com.sb10.mopl.watchingsession.repository.WatchingSessionRepository;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 유저의 첫 시청 세션 생성을 별도 트랜잭션(REQUIRES_NEW)으로 격리한다.
 *
 * <p>watching_session.watcher_id는 유니크 제약이 걸려 있어, 같은 유저가 동시에 두 탭에서 처음 구독하면 (둘 다
 * WatchingSessionService.join()의 findByWatcherId에서 "없음"을 본 뒤) 한쪽의 저장이 유니크 제약을 위반할 수 있다. 이 저장을 호출자의
 * 트랜잭션과 분리해 두면, 실패하더라도 호출자의 트랜잭션은 그대로 유지되어 승자의 행을 다시 조회해 이동 로직으로 이어갈 수 있다.
 */
@Component
@RequiredArgsConstructor
class WatchingSessionCreator {

  private final WatchingSessionRepository watchingSessionRepository;
  private final UserRepository userRepository;
  private final ContentRepository contentRepository;
  private final WatchingSessionMapper watchingSessionMapper;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public WatchingSessionDto create(UUID watcherId, UUID contentId) {
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
    watchingSessionRepository.saveAndFlush(watchingSession);
    contentRepository.incrementWatcherCount(contentId);

    return watchingSessionMapper.toDto(watchingSession);
  }
}
