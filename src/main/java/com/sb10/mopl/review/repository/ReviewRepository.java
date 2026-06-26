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

  @Query(
      """
    SELECT COUNT(r) > 0
    FROM Review r
    WHERE r.targetContent.id = :contentId
      AND r.user.id = :userId
      """)
  boolean existsByTargetContentIdAndUserId(
      @Param("contentId") UUID contentId, @Param("userId") UUID userId);

  @Query(
      """
    SELECT r
    FROM Review r
    WHERE r.targetContent.id = :contentId
      AND r.user.id = :userId
      """)
  Optional<Review> findByTargetContentIdAndUserId(
      @Param("contentId") UUID contentId, @Param("userId") UUID userId);

  @Query(
      """
    SELECT r
    FROM Review r
    JOIN FETCH r.user u
    WHERE (:contentId IS NULL OR r.targetContent.id = :contentId)
      AND (
        :cursor IS NULL
        OR r.createdAt < :cursor
        OR (:idAfter IS NOT NULL AND r.createdAt = :cursor AND r.id > :idAfter)
      )
    ORDER BY r.createdAt DESC, r.id ASC
      """)
  List<Review> findAllByCursorDesc(
      @Param("contentId") UUID contentId,
      @Param("cursor") Instant cursor,
      @Param("idAfter") UUID idAfter,
      Pageable pageable);

  @Query(
      """
    SELECT COUNT(r)
    FROM Review r
    WHERE r.targetContent.id = :contentId
      """)
  long countByTargetContentId(@Param("contentId") UUID contentId);
}
