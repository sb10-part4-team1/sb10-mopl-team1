package com.sb10.mopl.batch.processor;

import com.sb10.mopl.batch.cache.TmdbGenreCache;
import com.sb10.mopl.batch.dto.TmdbContentDto;
import com.sb10.mopl.batch.mapper.TmdbContentMapper;
import com.sb10.mopl.content.entity.Content;
import com.sb10.mopl.content.entity.ContentTag;
import com.sb10.mopl.content.entity.ContentType;
import com.sb10.mopl.content.entity.Tag;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

/*
 * TMDB API로부터 수집된 DTO(TmdbContentDto)를 검증하고 Content 엔티티로 변환하는 배치 프로세서입니다.
 *
 * title/name이 모두 없는 데이터는 무효한 데이터로 취급하여 null을 반환해 스킵 처리합니다.
 * 스텝 시작 시점에 @BeforeStep 리스너를 통해 해당 스텝 타입에 맞는 장르 캐시를 미리 1회 초기화합니다.
 * 이후 process() 내에서는 DB 조회 없이 메모리 캐시만을 사용하여 안전하게 장르 태그 연관관계를 맺어줍니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TmdbItemProcessor implements ItemProcessor<TmdbContentDto, Content> {

  private final TmdbContentMapper tmdbContentMapper;
  private final TmdbGenreCache tmdbGenreCache;

  @BeforeStep
  public void beforeStep(StepExecution stepExecution) {
    String stepName = stepExecution.getStepName();
    ContentType type = stepName.contains("Movie") ? ContentType.MOVIE : ContentType.TV_SERIES;
    tmdbGenreCache.initialize(type);
  }

  @Override
  public Content process(TmdbContentDto dto) {
    if (dto.resolveTitle() == null) {
      log.warn("TMDB title/name 없음 스킵 - id: {}", dto.id());
      return null; // 프로세서가 null을 반환하면 해당 아이템은 라이터로 전달되지 않고 자동 스킵됩니다.
    }

    if (dto.id() == null) {
      log.warn("TMDB id(provider_id) 없음 스킵 - title: {}", dto.resolveTitle());
      return null;
    }

    Content content = tmdbContentMapper.toEntity(dto);

    // 1. 장르 ID가 없다면 바로 Content 반환(태그 연관관계 생성 안함)
    Set<Integer> genreIds = dto.genreIds();
    if (genreIds == null || genreIds.isEmpty()) {
      return content;
    }

    ContentType type = dto.isMovie() ? ContentType.MOVIE : ContentType.TV_SERIES;

    // 2. 캐시 맵에서 Tag 엔티티를 꺼내 ContentTag 연관관계 생성
    for (Integer genreId : genreIds) {
      Tag tag = tmdbGenreCache.resolveTag(genreId, type);
      if (tag == null) {
        continue; // 알 수 없는 장르 ID는 스킵 (경고 로그는 resolveTag 내부에서 기록)
      }
      ContentTag.create(content, tag);
    }

    return content;
  }
}
