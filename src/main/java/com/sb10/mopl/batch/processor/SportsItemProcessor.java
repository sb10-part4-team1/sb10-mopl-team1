package com.sb10.mopl.batch.processor;

import com.sb10.mopl.batch.dto.SportsContentDto;
import com.sb10.mopl.batch.mapper.SportsContentMapper;
import com.sb10.mopl.content.entity.Content;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

/** SportsDB API로부터 수집된 DTO(SportsContentDto)를 검증하고 Content 엔티티로 변환하는 배치 프로세서입니다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class SportsItemProcessor implements ItemProcessor<SportsContentDto, Content> {

  private final SportsContentMapper sportsContentMapper;

  @Override
  public Content process(SportsContentDto dto) {
    if (dto.strEvent() == null) {
      log.warn("SportsDB strEvent(title) 없음 스킵 - id: {}", dto.idEvent());
      return null;
    }

    if (dto.idEvent() == null) {
      log.warn("SportsDB idEvent(provider_id) 없음 스킵 - title: {}", dto.strEvent());
      return null;
    }

    return sportsContentMapper.toEntity(dto);
  }
}
