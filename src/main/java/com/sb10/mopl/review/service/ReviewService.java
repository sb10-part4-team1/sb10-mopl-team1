package com.sb10.mopl.review.service;

import com.sb10.mopl.review.dto.ReviewCreateRequest;
import com.sb10.mopl.review.dto.ReviewDto;
import java.util.UUID;

public interface ReviewService {

  ReviewDto create(ReviewCreateRequest request, UUID userId);

}
