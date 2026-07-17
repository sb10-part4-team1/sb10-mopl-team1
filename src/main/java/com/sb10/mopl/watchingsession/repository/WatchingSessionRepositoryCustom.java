package com.sb10.mopl.watchingsession.repository;

import com.sb10.mopl.watchingsession.dto.WatchingSessionSearchRequest;
import com.sb10.mopl.watchingsession.entity.WatchingSession;
import java.util.List;
import java.util.UUID;

public interface WatchingSessionRepositoryCustom {

  List<WatchingSession> search(UUID contentId, WatchingSessionSearchRequest request);

  long countByContentId(UUID contentId, WatchingSessionSearchRequest request);
}
