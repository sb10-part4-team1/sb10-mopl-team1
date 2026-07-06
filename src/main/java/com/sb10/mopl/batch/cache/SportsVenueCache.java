package com.sb10.mopl.batch.cache;

import com.sb10.mopl.content.entity.Tag;
import com.sb10.mopl.content.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/*
 * Sports 경기장(venue) 온디맨드(On-demand) 캐시 컴포넌트입니다.
 *
 * 스프링 캐시(@Cacheable) 인터페이스와 Caffeine 캐시 매니저를 연동하여 동작합니다.
 * 트랜잭션 커밋 완료 후에만 캐시에 적재되는 TransactionAware 캐시 설정을 타게 되므로 롤백에 안전합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SportsVenueCache {

  private final TagRepository tagRepository;

  /*
   * 경기장명을 받아 해당하는 Tag 엔티티를 반환합니다.
   * 캐시("sportsVenues")에 있으면 데이터베이스를 거치지 않고 캐시에서 즉시 반환하고,
   * 없으면 데이터베이스를 조회(없을 시 신규 생성)하여 결과를 반환하고 캐시에 자동으로 누적합니다.
   */
  @Cacheable(value = "sportsVenues", cacheManager = "batchCacheManager")
  @Transactional
  public Tag resolveTag(String venueName) {
    return tagRepository
        .findByName(venueName)
        .orElseGet(
            () -> {
              Tag newTag = tagRepository.save(Tag.create(venueName));
              log.info("Sports 신규 경기장 태그 등록 - 경기장명: {}", venueName);
              return newTag;
            });
  }
}
