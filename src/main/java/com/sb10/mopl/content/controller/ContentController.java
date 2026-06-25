package com.sb10.mopl.content.controller;

import com.sb10.mopl.content.dto.ContentCreateRequest;
import com.sb10.mopl.content.dto.ContentDto;
import com.sb10.mopl.content.service.ContentService;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
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
  public ResponseEntity<ContentDto> createContent(
      @RequestPart("request") @Valid ContentCreateRequest request,
      @RequestPart(value = "thumbnail") MultipartFile thumbnail) {
    ContentDto contentDto = contentService.createContent(request, thumbnail);
    URI location = URI.create("/api/content/" + contentDto.id());
    return ResponseEntity.created(location).body(contentDto);
  }
}
