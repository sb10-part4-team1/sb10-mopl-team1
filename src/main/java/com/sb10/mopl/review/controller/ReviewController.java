package com.sb10.mopl.review.controller;

import com.sb10.mopl.review.dto.ReviewCreateRequest;
import com.sb10.mopl.review.dto.ReviewDto;
import com.sb10.mopl.review.service.ReviewService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reviews")
public class ReviewController {

  private final ReviewService reviewService;

  @PostMapping
  public ResponseEntity<ReviewDto> create(
      // TODO: 인증 구현 완료 후 X-USER-ID 헤더 대신 SecurityContext/JWT Principal에서 사용자 ID를 조회하도록 변경
      @Valid @RequestBody ReviewCreateRequest request, @RequestHeader("X-USER-ID") UUID userId) {
    ReviewDto response = reviewService.create(request, userId);

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }
}
