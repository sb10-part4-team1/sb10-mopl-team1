package com.sb10.mopl.watchingsession.repository;

import com.sb10.mopl.watchingsession.entity.WatchingSession;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WatchingSessionRepository
    extends JpaRepository<WatchingSession, UUID>, WatchingSessionRepositoryCustom {

  Optional<WatchingSession> findTopByWatcherIdOrderByCreatedAtDesc(UUID watcherId);

  Optional<WatchingSession> findByWatcherId(UUID watcherId);

  Optional<WatchingSession> findByWatcherIdAndContentId(UUID watcherId, UUID contentId);
}
