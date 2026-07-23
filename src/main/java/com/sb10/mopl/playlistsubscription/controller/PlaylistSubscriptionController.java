package com.sb10.mopl.playlistsubscription.controller;

import com.sb10.mopl.auth.security.user.AuthenticatedUser;
import com.sb10.mopl.auth.security.user.CurrentUser;
import com.sb10.mopl.playlistsubscription.controller.api.PlaylistSubscriptionControllerApiDocs;
import com.sb10.mopl.playlistsubscription.service.PlaylistSubscriptionService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/playlists")
public class PlaylistSubscriptionController implements PlaylistSubscriptionControllerApiDocs {

  private final PlaylistSubscriptionService playlistSubscriptionService;

  // 플레이리스트 구독
  @Override
  @PostMapping("/{playlistId}/subscription")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void subscribePlaylist(
      @PathVariable UUID playlistId, @CurrentUser AuthenticatedUser currentUser) {
    playlistSubscriptionService.subscribe(currentUser.id(), playlistId);
  }

  // 플레이리스트 구독 취소
  @Override
  @DeleteMapping("/{playlistId}/subscription")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void unsubscribePlaylist(
      @PathVariable UUID playlistId, @CurrentUser AuthenticatedUser currentUser) {
    playlistSubscriptionService.unsubscribe(currentUser.id(), playlistId);
  }
}
