package com.sb10.mopl.config;

import java.util.Optional;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import net.javacrumbs.shedlock.provider.redis.spring.RedisLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/*
 * 다중 서버 환경에서 동일한 스케줄러 작업이 중복 기동되지 않도록 제어하는 ShedLock 설정 클래스입니다.
 * Redis를 잠금 저장소로 삼아 분산 환경의 스케줄 락을 관리합니다.
 */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
public class ShedLockConfig {

  // 운영 및 개발 분산 환경용 Redis 락 프로바이더
  @Bean
  @Profile({"prod", "dev"})
  public LockProvider lockProvider(RedisConnectionFactory connectionFactory) {
    return new RedisLockProvider(connectionFactory, "mopl-lock");
  }

  // 로컬 및 테스트 빌드용 가짜(Mock) 락 프로바이더
  @Bean
  @Profile({"local", "test", "default"})
  public LockProvider mockLockProvider() {
    return new LockProvider() {
      @Override
      public Optional<SimpleLock> lock(LockConfiguration lockConfiguration) {
        return Optional.of(
            new SimpleLock() {
              @Override
              public void unlock() {
                // 아무 작업도 하지 않음
              }
            });
      }
    };
  }
}
