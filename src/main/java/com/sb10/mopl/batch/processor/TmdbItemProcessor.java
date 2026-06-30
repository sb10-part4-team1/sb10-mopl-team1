package com.sb10.mopl.batch.processor;

import com.sb10.mopl.batch.dto.TmdbContentDto;
import com.sb10.mopl.batch.mapper.TmdbContentMapper;
import com.sb10.mopl.content.entity.Content;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

/**
 * TMDB API로부터 수집된 DTO(TmdbContentDto)를 검증하고 Content 엔티티로 변환하는 배치 프로세서입니다.
 *
 * <p>title/name이 모두 없는 데이터는 무효한 데이터로 취급하여 null을 반환해 스킵 처리합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TmdbItemProcessor implements ItemProcessor<TmdbContentDto, Content> {

  private final TmdbContentMapper tmdbContentMapper;

  @Override
  public Content process(TmdbContentDto dto) {
    if (dto.resolveTitle() == null) {
      log.warn("TMDB title/name 없음 스킵 - id: {}", dto.id());
      return null; // 프로세서가 null을 반환하면 해당 아이템은 라이터로 전달되지 않고 자동 스킵됩니다.
    }

    return tmdbContentMapper.toEntity(dto);
  }
}
