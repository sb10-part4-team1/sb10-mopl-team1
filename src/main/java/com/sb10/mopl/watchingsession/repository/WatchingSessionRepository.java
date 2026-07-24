package com.sb10.mopl.watchingsession.repository;

import com.sb10.mopl.watchingsession.entity.WatchingSession;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

@Repository
public interface WatchingSessionRepository
    extends JpaRepository<WatchingSession, UUID>, WatchingSessionRepositoryCustom {

  Optional<WatchingSession> findTopByWatcherIdOrderByCreatedAtDesc(UUID watcherId);

  // WatchingSessionService.join()에서 같은 유저의 기존 세션을 이동시키기 전에 행을 잠가, 서로 다른 탭에서
  // 동시에 다른 콘텐츠로 이동시키는 경합(watcherCount 이중 반영)을 막는다.
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<WatchingSession> findByWatcherId(UUID watcherId);

  Optional<WatchingSession> findByWatcherIdAndContentId(UUID watcherId, UUID contentId);
}
