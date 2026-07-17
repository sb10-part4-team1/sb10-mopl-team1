package com.sb10.mopl.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.sb10.mopl.auth.entity.JwtSession;
import com.sb10.mopl.config.JpaAuditingConfig;
import com.sb10.mopl.config.QuerydslConfig;
import com.sb10.mopl.user.entity.User;
import com.sb10.mopl.user.repository.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest(
    properties = {"spring.sql.init.mode=never", "spring.jpa.hibernate.ddl-auto=create-drop"})
@Import({JpaAuditingConfig.class, QuerydslConfig.class})
class JwtSessionRepositoryTest {

  private static final Instant NOW = Instant.parse("2026-07-17T00:00:00Z");

  @Autowired private JwtSessionRepository jwtSessionRepository;

  @Autowired private UserRepository userRepository;

  @Test
  @DisplayName("만료 시각이 기준 시각 이전이거나 같은 세션만 삭제하고 이후 세션은 남긴다")
  void deleteByExpiresAtLessThanEqual_deletesOnlyExpiredOrEqualSessions() {
    // given
    JwtSession expiredSession = saveSession("expired-user", NOW.minusSeconds(60));
    JwtSession boundarySession = saveSession("boundary-user", NOW);
    JwtSession futureSession = saveSession("future-user", NOW.plusSeconds(60));

    // when
    int deletedCount = jwtSessionRepository.deleteByExpiresAtLessThanEqual(NOW);

    // then
    assertThat(deletedCount).isEqualTo(2);
    assertThat(jwtSessionRepository.findById(expiredSession.getId())).isEmpty();
    assertThat(jwtSessionRepository.findById(boundarySession.getId())).isEmpty();
    assertThat(jwtSessionRepository.findById(futureSession.getId())).isPresent();
  }

  private JwtSession saveSession(String userName, Instant expiresAt) {
    User user = User.createUser(userName, userName + "@example.com", "encoded-password", null);
    userRepository.saveAndFlush(user);
    JwtSession session = JwtSession.create(user, UUID.randomUUID(), expiresAt);
    return jwtSessionRepository.saveAndFlush(session);
  }
}
