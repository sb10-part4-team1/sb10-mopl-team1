package com.sb10.mopl.playlist.repository;

import com.sb10.mopl.playlist.entity.Playlist;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlaylistRepository extends JpaRepository<Playlist, UUID> {

  @Query(
      """
  SELECT p
  FROM Playlist p
  JOIN FETCH p.owner o
  WHERE (:ownerId IS NULL OR p.owner.id = :ownerId)
    AND (
      :keywordLike IS NULL
      OR LOWER(p.title) LIKE LOWER(CONCAT('%', :keywordLike, '%'))
      OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keywordLike, '%'))
    )
    AND (
      :cursor IS NULL
      OR p.updatedAt < :cursor
      OR (:idAfter IS NOT NULL AND p.updatedAt = :cursor AND p.id > :idAfter)
    )
  ORDER BY p.updatedAt DESC, p.id ASC
      """)
  List<Playlist> findAllByUpdatedAtCursorDesc(
      @Param("keywordLike") String keywordLike,
      @Param("ownerId") UUID ownerId,
      @Param("cursor") Instant cursor,
      @Param("idAfter") UUID idAfter,
      Pageable pageable);

  @Query(
      """
  SELECT COUNT(p)
  FROM Playlist p
  WHERE (:ownerId IS NULL OR p.owner.id = :ownerId)
    AND (
      :keywordLike IS NULL
      OR LOWER(p.title) LIKE LOWER(CONCAT('%', :keywordLike, '%'))
      OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keywordLike, '%'))
    )
      """)
  long countBySearchCondition(
      @Param("keywordLike") String keywordLike, @Param("ownerId") UUID ownerId);

  @Query(
      """
  SELECT p
  FROM Playlist p
  JOIN FETCH p.owner
  WHERE p.id = :playlistId
      """)
  Optional<Playlist> findByIdWithOwner(@Param("playlistId") UUID playlistId);

  @Query(
      """
  SELECT p
  FROM PlaylistSubscription ps
  JOIN ps.playlist p
  JOIN FETCH p.owner
  WHERE ps.subscriber.id = :subscriberId
    AND (:ownerId IS NULL OR p.owner.id = :ownerId)
    AND (
      :keywordLike IS NULL
      OR LOWER(p.title) LIKE LOWER(CONCAT('%', :keywordLike, '%'))
      OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keywordLike, '%'))
    )
    AND (
      :cursor IS NULL
      OR p.updatedAt < :cursor
      OR (:idAfter IS NOT NULL AND p.updatedAt = :cursor AND p.id > :idAfter)
    )
  ORDER BY p.updatedAt DESC, p.id ASC
      """)
  List<Playlist> findSubscribedByUpdatedAtCursorDesc(
      @Param("keywordLike") String keywordLike,
      @Param("ownerId") UUID ownerId,
      @Param("subscriberId") UUID subscriberId,
      @Param("cursor") Instant cursor,
      @Param("idAfter") UUID idAfter,
      Pageable pageable);

  @Query(
      """
  SELECT COUNT(ps)
  FROM PlaylistSubscription ps
  JOIN ps.playlist p
  WHERE ps.subscriber.id = :subscriberId
    AND (:ownerId IS NULL OR p.owner.id = :ownerId)
    AND (
      :keywordLike IS NULL
      OR LOWER(p.title) LIKE LOWER(CONCAT('%', :keywordLike, '%'))
      OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keywordLike, '%'))
    )
      """)
  long countSubscribedBySearchCondition(
      @Param("keywordLike") String keywordLike,
      @Param("ownerId") UUID ownerId,
      @Param("subscriberId") UUID subscriberId);

  @Query(
      """
  SELECT p
  FROM Playlist p
  JOIN FETCH p.owner
  WHERE (:ownerId IS NULL OR p.owner.id = :ownerId)
    AND (
      :keywordLike IS NULL
      OR LOWER(p.title) LIKE LOWER(CONCAT('%', :keywordLike, '%'))
      OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keywordLike, '%'))
    )
    AND (
      :subscriberCountCursor IS NULL
      OR (
        SELECT COUNT(psCount)
        FROM PlaylistSubscription psCount
        WHERE psCount.playlist = p
      ) < :subscriberCountCursor
      OR (
        :idAfter IS NOT NULL
        AND (
          SELECT COUNT(psTie)
          FROM PlaylistSubscription psTie
          WHERE psTie.playlist = p
        ) = :subscriberCountCursor
        AND p.id > :idAfter
      )
    )
  ORDER BY (
    SELECT COUNT(psOrder)
    FROM PlaylistSubscription psOrder
    WHERE psOrder.playlist = p
  ) DESC, p.id ASC
      """)
  List<Playlist> findAllBySubscriberCountCursorDesc(
      @Param("keywordLike") String keywordLike,
      @Param("ownerId") UUID ownerId,
      @Param("subscriberCountCursor") Long subscriberCountCursor,
      @Param("idAfter") UUID idAfter,
      Pageable pageable);

  @Query(
      """
  SELECT p
  FROM PlaylistSubscription filterPs
  JOIN filterPs.playlist p
  JOIN FETCH p.owner
  WHERE filterPs.subscriber.id = :subscriberId
    AND (:ownerId IS NULL OR p.owner.id = :ownerId)
    AND (
      :keywordLike IS NULL
      OR LOWER(p.title) LIKE LOWER(CONCAT('%', :keywordLike, '%'))
      OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keywordLike, '%'))
    )
    AND (
      :subscriberCountCursor IS NULL
      OR (
        SELECT COUNT(psCount)
        FROM PlaylistSubscription psCount
        WHERE psCount.playlist = p
      ) < :subscriberCountCursor
      OR (
        :idAfter IS NOT NULL
        AND (
          SELECT COUNT(psTie)
          FROM PlaylistSubscription psTie
          WHERE psTie.playlist = p
        ) = :subscriberCountCursor
        AND p.id > :idAfter
      )
    )
  ORDER BY (
    SELECT COUNT(psOrder)
    FROM PlaylistSubscription psOrder
    WHERE psOrder.playlist = p
  ) DESC, p.id ASC
      """)
  List<Playlist> findSubscribedBySubscriberCountCursorDesc(
      @Param("keywordLike") String keywordLike,
      @Param("ownerId") UUID ownerId,
      @Param("subscriberId") UUID subscriberId,
      @Param("subscriberCountCursor") Long subscriberCountCursor,
      @Param("idAfter") UUID idAfter,
      Pageable pageable);
}
