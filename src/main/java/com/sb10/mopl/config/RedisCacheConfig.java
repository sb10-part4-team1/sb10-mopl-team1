package com.sb10.mopl.config;

import java.time.Duration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.transaction.TransactionAwareCacheManagerProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/*
 * 운영 및 배포(AWS) 환경 전용 Redis 기반 분산 캐시 설정 클래스입니다.
 */
@Configuration
@EnableCaching
@Profile({"prod", "dev"})
public class RedisCacheConfig {

  // 배치 가공 전용 트랜잭션 인지형 Redis 캐시 매니저
  @Bean
  public CacheManager batchCacheManager(RedisConnectionFactory connectionFactory) {
    RedisCacheConfiguration defaultConfig = createRedisCacheConfig(Duration.ofMinutes(30));

    RedisCacheManager redisCacheManager =
        RedisCacheManager.builder(connectionFactory).cacheDefaults(defaultConfig).build();

    return new TransactionAwareCacheManagerProxy(redisCacheManager);
  }

  // 일반 API 조회 서비스용 기본 Redis 캐시 매니저 (기본 적용)
  @Primary
  @Bean
  public CacheManager queryCacheManager(RedisConnectionFactory connectionFactory) {
    RedisCacheConfiguration defaultConfig = createRedisCacheConfig(Duration.ofHours(24));

    return RedisCacheManager.builder(connectionFactory).cacheDefaults(defaultConfig).build();
  }

  private RedisCacheConfiguration createRedisCacheConfig(Duration ttl) {
    return RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(ttl)
        .disableCachingNullValues()
        .serializeKeysWith(
            RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
        .serializeValuesWith(
            RedisSerializationContext.SerializationPair.fromSerializer(
                new GenericJackson2JsonRedisSerializer()));
  }
}
