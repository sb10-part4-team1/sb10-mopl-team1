package com.sb10.mopl.content.controller;

import com.sb10.mopl.common.pagination.CursorPageResponse;
import com.sb10.mopl.content.dto.ContentCreateRequest;
import com.sb10.mopl.content.dto.ContentDto;
import com.sb10.mopl.content.dto.ContentSearchRequest;
import com.sb10.mopl.content.dto.ContentUpdateRequest;
import com.sb10.mopl.content.service.ContentService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/content")
@RequiredArgsConstructor
public class ContentController {

  private final ContentService contentService;

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ContentDto> create(
      @RequestPart("request") @Valid ContentCreateRequest request,
      @RequestPart(value = "thumbnail") MultipartFile thumbnail) {
    ContentDto contentDto = contentService.create(request, thumbnail);
    URI location = URI.create("/api/content/" + contentDto.id());
    return ResponseEntity.created(location).body(contentDto);
  }

  @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ContentDto> update(
      @PathVariable UUID id,
      @RequestPart("request") @Valid ContentUpdateRequest request,
      @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail) {
    ContentDto contentDto = contentService.update(id, request, thumbnail);
    return ResponseEntity.ok(contentDto);
  }

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
}
