package com.sb10.mopl.auth.repository;

import com.sb10.mopl.auth.entity.TemporaryPassword;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TemporaryPasswordRepository extends JpaRepository<TemporaryPassword, UUID> {

  @Query(
      """
      SELECT temporaryPassword
      FROM TemporaryPassword temporaryPassword
      JOIN FETCH temporaryPassword.user user
      WHERE user.email = :email
        AND user.isDeleted = false
      """)
  Optional<TemporaryPassword> findByUserEmailWithUser(@Param("email") String email);

  @Modifying(flushAutomatically = true)
  @Query(
      """
      DELETE FROM TemporaryPassword temporaryPassword
      WHERE temporaryPassword.user.id = :userId
      """)
  int deleteByUserId(@Param("userId") UUID userId);

  @Modifying(flushAutomatically = true)
  @Query(
      """
      DELETE FROM TemporaryPassword temporaryPassword
      WHERE temporaryPassword.id = :temporaryPasswordId
        AND temporaryPassword.user.id = :userId
      """)
  int deleteByIdAndUserId(
      @Param("temporaryPasswordId") UUID temporaryPasswordId, @Param("userId") UUID userId);
}
