package com.sb10.mopl.content.controller;

import com.sb10.mopl.common.pagination.CursorPageResponse;
import com.sb10.mopl.content.dto.ContentCreateRequest;
import com.sb10.mopl.content.dto.ContentDto;
import com.sb10.mopl.content.dto.ContentSearchRequest;
import com.sb10.mopl.content.dto.ContentUpdateRequest;
import com.sb10.mopl.content.service.ContentService;
import com.sb10.mopl.watchingsession.dto.WatchingSessionDto;
import com.sb10.mopl.watchingsession.dto.WatchingSessionSearchRequest;
import com.sb10.mopl.watchingsession.service.WatchingSessionService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/contents")
@RequiredArgsConstructor
public class ContentController {

  private final ContentService contentService;
  private final WatchingSessionService watchingSessionService;

  @PreAuthorize("hasRole('ADMIN')")
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ContentDto> create(
      @RequestPart("request") @Valid ContentCreateRequest request,
      @RequestPart(value = "thumbnail") MultipartFile thumbnail) {
    ContentDto contentDto = contentService.create(request, thumbnail);
    URI location = URI.create("/api/contents/" + contentDto.id());
    return ResponseEntity.created(location).body(contentDto);
  }

  @PreAuthorize("hasRole('ADMIN')")
  @PatchMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ContentDto> update(
      @PathVariable UUID id,
      @RequestPart("request") @Valid ContentUpdateRequest request,
      @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail) {
    ContentDto contentDto = contentService.update(id, request, thumbnail);
    return ResponseEntity.ok(contentDto);
  }

  @PreAuthorize("hasRole('ADMIN')")
  @DeleteMapping(value = "/{id}")
  public ResponseEntity<Void> delete(@PathVariable UUID id) {
    contentService.delete(id);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{id}")
  public ResponseEntity<ContentDto> find(@PathVariable UUID id) {
    ContentDto contentDto = contentService.find(id);
    return ResponseEntity.ok(contentDto);
  }

  @GetMapping
  public ResponseEntity<CursorPageResponse<ContentDto>> findAll(
      @ModelAttribute @Valid ContentSearchRequest request) {
    CursorPageResponse<ContentDto> response = contentService.findAll(request);
    return ResponseEntity.ok(response);
  }

  // 특정 콘텐츠의 시청 세션(현재 시청자) 목록 조회
  @GetMapping("/{contentId}/watching-sessions")
  public ResponseEntity<CursorPageResponse<WatchingSessionDto>> findWatchingSessions(
      @PathVariable UUID contentId, @ModelAttribute @Valid WatchingSessionSearchRequest request) {
    CursorPageResponse<WatchingSessionDto> response =
        watchingSessionService.findByContent(contentId, request);
    return ResponseEntity.ok(response);
  }
}
