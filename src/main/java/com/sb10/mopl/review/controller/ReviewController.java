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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reviews")
public class ReviewController {

  private final ReviewService reviewService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ResponseEntity<ReviewDto> create(
    @Valid @RequestBody ReviewCreateRequest request,
    @RequestHeader("X-USER-ID") UUID userId
  ) {
    ReviewDto response = reviewService.create(request, userId);

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

}
