package com.sb10.mopl.watchingsession.controller;

import com.sb10.mopl.common.pagination.CursorPageResponse;
import com.sb10.mopl.watchingsession.controller.api.WatchingSessionControllerApiDocs;
import com.sb10.mopl.watchingsession.dto.WatchingSessionDto;
import com.sb10.mopl.watchingsession.dto.WatchingSessionSearchRequest;
import com.sb10.mopl.watchingsession.service.WatchingSessionService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class WatchingSessionController implements WatchingSessionControllerApiDocs {

  private final WatchingSessionService watchingSessionService;

  @Override
  @GetMapping("/api/users/{watcherId}/watching-sessions")
  public ResponseEntity<WatchingSessionDto> findWatchingSession(@PathVariable UUID watcherId) {
    return watchingSessionService
        .findLatestByWatcher(watcherId)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.noContent().build());
  }

  @Override
  @GetMapping("/api/contents/{contentId}/watching-sessions")
  public ResponseEntity<CursorPageResponse<WatchingSessionDto>> findWatchingSessions(
      @PathVariable UUID contentId, @ModelAttribute @Valid WatchingSessionSearchRequest request) {
    CursorPageResponse<WatchingSessionDto> response =
        watchingSessionService.findByContent(contentId, request);
    return ResponseEntity.ok(response);
  }
}
