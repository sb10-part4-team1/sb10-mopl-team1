package com.sb10.mopl.auth.repository;

import com.sb10.mopl.auth.entity.RefreshToken;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

  @Query(
      """
      SELECT token
      FROM RefreshToken token
      JOIN FETCH token.user
      WHERE token.tokenHash = :tokenHash
      """)
  Optional<RefreshToken> findByTokenHashWithUser(@Param("tokenHash") String tokenHash);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      """
      DELETE FROM RefreshToken token
      WHERE token.tokenHash = :tokenHash
      """)
  int deleteByTokenHash(@Param("tokenHash") String tokenHash);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      """
      DELETE FROM RefreshToken token
      WHERE token.user.id = :userId
      """)
  int deleteByUserId(@Param("userId") UUID userId);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      """
      DELETE FROM RefreshToken token
      WHERE token.expiresAt <= :now
      """)
  int deleteByExpiresAtLessThanEqual(@Param("now") Instant now);
}
