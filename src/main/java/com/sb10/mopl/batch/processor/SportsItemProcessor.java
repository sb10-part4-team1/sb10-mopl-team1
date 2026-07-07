package com.sb10.mopl.batch.processor;

import com.sb10.mopl.batch.cache.SportsVenueCache;
import com.sb10.mopl.batch.dto.SportsContentDto;
import com.sb10.mopl.batch.mapper.SportsContentMapper;
import com.sb10.mopl.content.entity.Content;
import com.sb10.mopl.content.entity.ContentTag;
import com.sb10.mopl.content.entity.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/*
 * SportsDB API로부터 수집된 DTO(SportsContentDto)를 검증하고 Content 엔티티로 변환하는 배치 프로세서입니다.
 * strVenue(경기장명)가 존재하면 SportsVenueCache를 통해 Tag 엔티티를 조회/생성하여
 * ContentTag 연관관계를 맺습니다. 경기장 정보가 없는 경기는 태그 없이 저장됩니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SportsItemProcessor implements ItemProcessor<SportsContentDto, Content> {

  private final SportsContentMapper sportsContentMapper;
  private final SportsVenueCache sportsVenueCache;

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

    Content content = sportsContentMapper.toEntity(dto);

    // 경기장명이 있는 경기만 캐시에서 Tag 조회/생성 후 ContentTag 연관관계 생성
    if (StringUtils.hasText(dto.strVenue())) {
      Tag tag = sportsVenueCache.resolveTag(dto.strVenue());
      ContentTag.create(content, tag);
    }

    return content;
  }
}
