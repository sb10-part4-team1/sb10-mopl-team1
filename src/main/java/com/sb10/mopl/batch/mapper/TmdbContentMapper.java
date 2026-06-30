package com.sb10.mopl.batch.mapper;

import com.sb10.mopl.batch.dto.TmdbContentDto;
import com.sb10.mopl.content.entity.Content;
import com.sb10.mopl.content.entity.ContentType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** TMDB 인기 콘텐츠 DTO 객체(TmdbContentDto)를 데이터베이스 엔티티(Content)로 변환해 주는 매퍼 클래스입니다. */
@Component
public class TmdbContentMapper {

  private static final String TMDB_IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500";
  private static final String DEFAULT_THUMBNAIL_URL = "/uploads/default-thumbnail.png";
  private static final String DEFAULT_DESCRIPTION = "설명이 없는 컨텐츠입니다.";

  /**
   * TMDB DTO를 Content 도메인 엔티티로 매핑하여 반환합니다.
   *
   * @param dto TMDB API 응답 DTO
   * @return 가공 및 도메인 제약조건 검증이 완료된 Content 엔티티
   */
  public Content toEntity(TmdbContentDto dto) {
    String thumbnailUrl =
        StringUtils.hasText(dto.posterPath()) // 값이 없으면 기본 URL으로 설정
            ? TMDB_IMAGE_BASE_URL + dto.posterPath()
            : DEFAULT_THUMBNAIL_URL;

    // 값이 없으면 기본 문구로 설정
    String description = StringUtils.hasText(dto.overview()) ? dto.overview() : DEFAULT_DESCRIPTION;

    ContentType type = dto.isMovie() ? ContentType.MOVIE : ContentType.TV_SERIES;

    return Content.create(dto.resolveTitle(), type, description, thumbnailUrl);
  }
}
