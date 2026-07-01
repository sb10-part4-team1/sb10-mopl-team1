package com.sb10.mopl.playlist.controller;

import com.sb10.mopl.auth.security.user.AuthenticatedUser;
import com.sb10.mopl.auth.security.user.CurrentUser;
import com.sb10.mopl.common.pagination.CursorPageResponse;
import com.sb10.mopl.common.pagination.SortDirection;
import com.sb10.mopl.playlist.dto.PlaylistCreateRequest;
import com.sb10.mopl.playlist.dto.PlaylistDto;
import com.sb10.mopl.playlist.service.PlaylistService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

  @GetMapping
  public ResponseEntity<CursorPageResponse<PlaylistDto>> findAll(
      @RequestParam(required = false) String keywordLike,
      @RequestParam(required = false) UUID ownerIdEqual,
      @RequestParam(required = false) UUID subscriberIdEqual,
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false) UUID idAfter,
      @RequestParam Integer limit,
      @RequestParam String sortBy,
      @RequestParam SortDirection sortDirection) {

    CursorPageResponse<PlaylistDto> response =
        playlistService.findAll(
            keywordLike,
            ownerIdEqual,
            subscriberIdEqual,
            cursor,
            idAfter,
            limit,
            sortBy,
            sortDirection);

    return ResponseEntity.ok(response);
  }

  @GetMapping("/{playlistId}")
  public ResponseEntity<PlaylistDto> findById(@PathVariable UUID playlistId) {
    PlaylistDto response = playlistService.findById(playlistId);
    return ResponseEntity.ok(response);
  }
}
