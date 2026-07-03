package com.sb10.mopl.auth.repository;

import com.sb10.mopl.auth.entity.JwtSession;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JwtSessionRepository extends JpaRepository<JwtSession, UUID> {

  @Query(
      """
      SELECT session
      FROM JwtSession session
      WHERE session.user.id = :userId
        AND session.expiresAt > :now
      """)
  Optional<JwtSession> findActiveByUserId(@Param("userId") UUID userId, @Param("now") Instant now);

  @Query(
      """
      SELECT COUNT(session) > 0
      FROM JwtSession session
      WHERE session.user.id = :userId
        AND session.sessionId = :sessionId
        AND session.expiresAt > :now
      """)
  boolean existsActiveSession(
      @Param("userId") UUID userId, @Param("sessionId") UUID sessionId, @Param("now") Instant now);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      """
      DELETE FROM JwtSession session
      WHERE session.user.id = :userId
      """)
  int deleteByUserId(@Param("userId") UUID userId);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      """
      DELETE FROM JwtSession session
      WHERE session.expiresAt <= :now
      """)
  int deleteByExpiresAtLessThanEqual(@Param("now") Instant now);
}
