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
}
