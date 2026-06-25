package com.sb10.mopl.review.repository;

import com.sb10.mopl.review.entity.Review;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

  boolean existsByTargetContentIdAndUserId(UUID contentId, UUID userId);

  Optional<Review> findByTargetContentIdAndUserId(UUID contentId, UUID userId);

  @Query(
      """
      SELECT r
      FROM Review r
      JOIN FETCH r.user u
      WHERE (:contentId IS NULL OR r.targetContent.id = :contentId)
        AND (
          :cursor IS NULL
          OR r.createdAt < :cursor
          OR (r.createdAt = :cursor AND r.id > :idAfter)
        )
      ORDER BY r.createdAt DESC, r.id ASC
      """)
  List<Review> findAllByCursorDesc(
      @Param("contentId") UUID contentId,
      @Param("cursor") Instant cursor,
      @Param("idAfter") UUID idAfter,
      Pageable pageable);

  long countByTargetContentId(UUID contentId);
}
