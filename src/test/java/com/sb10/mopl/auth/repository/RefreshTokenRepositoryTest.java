package com.sb10.mopl.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.sb10.mopl.auth.entity.RefreshToken;
import com.sb10.mopl.config.JpaAuditingConfig;
import com.sb10.mopl.config.QuerydslConfig;
import com.sb10.mopl.user.entity.User;
import com.sb10.mopl.user.repository.UserRepository;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest(
    properties = {"spring.sql.init.mode=never", "spring.jpa.hibernate.ddl-auto=create-drop"})
@Import({JpaAuditingConfig.class, QuerydslConfig.class})
class RefreshTokenRepositoryTest {

  private static final Instant NOW = Instant.parse("2026-07-17T00:00:00Z");

  @Autowired private RefreshTokenRepository refreshTokenRepository;

  @Autowired private UserRepository userRepository;

  @Test
  @DisplayName("만료 시각이 기준 시각 이전이거나 같은 토큰만 삭제하고 이후 토큰은 남긴다")
  void deleteByExpiresAtLessThanEqual_deletesOnlyExpiredOrEqualTokens() {
    // given
    RefreshToken expiredToken = saveToken("expired-user", NOW.minusSeconds(60));
    RefreshToken boundaryToken = saveToken("boundary-user", NOW);
    RefreshToken futureToken = saveToken("future-user", NOW.plusSeconds(60));

    // when
    int deletedCount = refreshTokenRepository.deleteByExpiresAtLessThanEqual(NOW);

    // then
    assertThat(deletedCount).isEqualTo(2);
    assertThat(refreshTokenRepository.findById(expiredToken.getId())).isEmpty();
    assertThat(refreshTokenRepository.findById(boundaryToken.getId())).isEmpty();
    assertThat(refreshTokenRepository.findById(futureToken.getId())).isPresent();
  }

  private RefreshToken saveToken(String userName, Instant expiresAt) {
    User user = User.createUser(userName, userName + "@example.com", "encoded-password", null);
    userRepository.saveAndFlush(user);
    RefreshToken token = RefreshToken.create(user, "hash-" + userName, expiresAt);
    return refreshTokenRepository.saveAndFlush(token);
  }
}
