package com.sb10.mopl.review.repository;

import com.sb10.mopl.review.entity.Review;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface ReviewRepositoryCustom {

  boolean existsByTargetContentIdAndUserId(UUID contentId, UUID userId);

  Optional<Review> findByTargetContentIdAndUserId(UUID contentId, UUID userId);

  List<Review> findAllByCursorDesc(UUID contentId, Instant cursor, UUID idAfter, Pageable pageable);

  long countByTargetContentId(UUID contentId);

  ReviewStatistics findStatisticsByTargetContentId(UUID contentId);
}
