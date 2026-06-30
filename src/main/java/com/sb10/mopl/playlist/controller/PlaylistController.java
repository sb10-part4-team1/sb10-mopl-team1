package com.sb10.mopl.playlist.controller;

import com.sb10.mopl.auth.security.user.AuthenticatedUser;
import com.sb10.mopl.auth.security.user.CurrentUser;
import com.sb10.mopl.playlist.dto.PlaylistCreateRequest;
import com.sb10.mopl.playlist.dto.PlaylistDto;
import com.sb10.mopl.playlist.service.PlaylistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/playlists")
@RequiredArgsConstructor
public class PlaylistController {

  private final PlaylistService playlistService;

  @PostMapping
  public ResponseEntity<PlaylistDto> create(
      @Valid @RequestBody PlaylistCreateRequest request,
      @CurrentUser AuthenticatedUser currentUser) {
    PlaylistDto response = playlistService.create(request, currentUser.id());

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }
}
