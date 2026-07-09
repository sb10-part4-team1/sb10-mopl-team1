package com.sb10.mopl.auth.repository;

import com.sb10.mopl.auth.entity.SocialAccount;
import com.sb10.mopl.auth.entity.SocialProvider;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SocialAccountRepository extends JpaRepository<SocialAccount, UUID> {

  @Query(
      """
      SELECT account
      FROM SocialAccount account
      JOIN FETCH account.user
      WHERE account.provider = :provider
        AND account.providerUserId = :providerUserId
      """)
  Optional<SocialAccount> findByProviderAndProviderUserIdWithUser(
      @Param("provider") SocialProvider provider, @Param("providerUserId") String providerUserId);
}
