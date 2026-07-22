package com.sb10.mopl.content.controller;

import com.sb10.mopl.common.pagination.CursorPageResponse;
import com.sb10.mopl.content.controller.api.ContentControllerApiDocs;
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
public class ContentController implements ContentControllerApiDocs {

  private final ContentService contentService;
  private final WatchingSessionService watchingSessionService;

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ContentDto> create(
      @RequestPart(value = "request") @Valid ContentCreateRequest request,
      @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail) {
    ContentDto contentDto = contentService.create(request, thumbnail);
    URI location = URI.create("/api/contents/" + contentDto.id());
    return ResponseEntity.created(location).body(contentDto);
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  @PatchMapping(value = "/{contentId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ContentDto> update(
      @PathVariable UUID contentId,
      @RequestPart("request") @Valid ContentUpdateRequest request,
      @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail) {
    ContentDto contentDto = contentService.update(contentId, request, thumbnail);
    return ResponseEntity.ok(contentDto);
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  @DeleteMapping(value = "/{contentId}")
  public ResponseEntity<Void> delete(@PathVariable UUID contentId) {
    contentService.delete(contentId);
    return ResponseEntity.noContent().build();
  }

  @Override
  @GetMapping("/{contentId}")
  public ResponseEntity<ContentDto> find(@PathVariable UUID contentId) {
    ContentDto contentDto = contentService.find(contentId);
    return ResponseEntity.ok(contentDto);
  }

  @Override
  @GetMapping
  public ResponseEntity<CursorPageResponse<ContentDto>> findAll(
      @ModelAttribute @Valid ContentSearchRequest request) {
    CursorPageResponse<ContentDto> response = contentService.findAll(request);
    return ResponseEntity.ok(response);
  }

  // 특정 콘텐츠의 시청 세션(현재 시청자) 목록 조회
  @Override
  @GetMapping("/{contentId}/watching-sessions")
  public ResponseEntity<CursorPageResponse<WatchingSessionDto>> findWatchingSessions(
      @PathVariable UUID contentId, @ModelAttribute @Valid WatchingSessionSearchRequest request) {
    CursorPageResponse<WatchingSessionDto> response =
        watchingSessionService.findByContent(contentId, request);
    return ResponseEntity.ok(response);
  }
}
