package com.sb10.mopl.batch.mapper;

import com.sb10.mopl.batch.dto.SportsContentDto;
import com.sb10.mopl.content.entity.Content;
import com.sb10.mopl.content.entity.ContentProvider;
import com.sb10.mopl.content.entity.ContentType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** SportsDB 경기 DTO 객체(SportsContentDto)를 데이터베이스 엔티티(Content)로 변환해 주는 매퍼 클래스입니다. */
@Component
public class SportsContentMapper {

  private static final String DEFAULT_THUMBNAIL_URL = "/uploads/default-thumbnail.png";
  private static final String DEFAULT_DESCRIPTION = "설명이 없는 스포츠 경기 콘텐츠입니다.";

  /**
   * Sports DTO를 Content 도메인 엔티티로 매핑하여 반환합니다.
   *
   * @param dto Sports API 응답 DTO
   * @return 가공 및 도메인 제약조건 검증이 완료된 Content 엔티티
   */
  public Content toEntity(SportsContentDto dto) {
    String thumbnailUrl =
        StringUtils.hasText(dto.strThumb()) ? dto.strThumb() : DEFAULT_THUMBNAIL_URL;

    String description =
        StringUtils.hasText(dto.strFilename()) ? dto.strFilename() : DEFAULT_DESCRIPTION;

    return Content.createWithProvider(
        dto.strEvent(),
        ContentType.SPORT,
        description,
        thumbnailUrl,
        ContentProvider.SPORTS_DB,
        String.valueOf(dto.idEvent()));
  }
}
