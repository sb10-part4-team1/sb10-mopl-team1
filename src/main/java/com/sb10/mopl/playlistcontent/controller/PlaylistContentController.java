package com.sb10.mopl.playlistcontent.controller;

import com.sb10.mopl.auth.security.user.AuthenticatedUser;
import com.sb10.mopl.auth.security.user.CurrentUser;
import com.sb10.mopl.playlistcontent.service.PlaylistContentService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/playlists/{playlistId}/contents")
@RequiredArgsConstructor
public class PlaylistContentController {

  private final PlaylistContentService playlistContentService;

  @PostMapping("/{contentId}")
  public ResponseEntity<Void> add(
      @PathVariable UUID playlistId,
      @PathVariable UUID contentId,
      @CurrentUser AuthenticatedUser currentUser) {
    playlistContentService.add(playlistId, contentId, currentUser.id());
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{contentId}")
  public ResponseEntity<Void> delete(
      @PathVariable UUID playlistId,
      @PathVariable UUID contentId,
      @CurrentUser AuthenticatedUser currentUser) {
    playlistContentService.delete(playlistId, contentId, currentUser.id());
    return ResponseEntity.noContent().build();
  }
}
