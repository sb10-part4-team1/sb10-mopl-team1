package com.sb10.mopl.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.transaction.TransactionAwareCacheManagerProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/*
 * 스프링 통합 캐시 설정 클래스입니다.
 *
 * 1. batchCacheManager: 배치 가공 전용 캐시 매니저로, 30분 만료 및 트랜잭션 롤백 방어(TransactionAware) 기능을 내장합니다.
 * 2. queryCacheManager: 일반 API 조회용 기본 캐시 매니저로, 24시간 캐시 유지 정책을 가지며 @Primary로 지정되어 기본 매칭됩니다.
 */
@Configuration
@EnableCaching
public class CacheConfig {

  // 배치 가공 전용 트랜잭션 인지형 캐시 매니저
  @Bean
  public CacheManager batchCacheManager() {
    CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();

    // 저장 최대 용량 10000개, 30분 뒤 만료
    caffeineCacheManager.setCaffeine(
        Caffeine.newBuilder().maximumSize(10000).expireAfterAccess(30, TimeUnit.MINUTES));

    caffeineCacheManager.setCacheNames(List.of("sportsVenues"));

    return new TransactionAwareCacheManagerProxy(caffeineCacheManager);
  }

  // 일반 API 조회 서비스용 기본 캐시 매니저 (지정 생략 시 기본 적용)
  @Primary
  @Bean
  public CacheManager queryCacheManager() {
    CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();

    // 저장 최대 용량 50000개, 24시간 뒤 만료
    caffeineCacheManager.setCaffeine(
        Caffeine.newBuilder().maximumSize(50000).expireAfterWrite(24, TimeUnit.HOURS));

    caffeineCacheManager.setCacheNames(List.of("contents", "playlists"));

    return caffeineCacheManager;
  }
}
