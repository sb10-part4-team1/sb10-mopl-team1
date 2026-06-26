package com.sb10.mopl.review.service;

import com.sb10.mopl.common.pagination.CursorPageResponse;
import com.sb10.mopl.common.pagination.SortDirection;
import com.sb10.mopl.review.dto.ReviewCreateRequest;
import com.sb10.mopl.review.dto.ReviewDto;
import java.util.UUID;

public interface ReviewService {

  ReviewDto create(ReviewCreateRequest request, UUID userId);

  // 리뷰 목록을 커서 페이지네이션 방식으로 조회
  CursorPageResponse<ReviewDto> findAll(
      UUID contentId,
      String cursor,
      UUID idAfter,
      Integer limit,
      String sortBy,
      SortDirection sortDirection);
}
