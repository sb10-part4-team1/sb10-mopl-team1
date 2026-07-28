package com.sb10.mopl.batch.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.sb10.mopl.batch.dto.TmdbContentDto;
import com.sb10.mopl.content.entity.Content;
import com.sb10.mopl.content.entity.ContentProvider;
import com.sb10.mopl.content.entity.ContentType;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class TmdbContentMapperTest {

  private TmdbContentMapper tmdbContentMapper;

  @BeforeEach
  void setUp() {
    tmdbContentMapper = new TmdbContentMapper();
    ReflectionTestUtils.setField(
        tmdbContentMapper, "defaultImageUrl", "/uploads/default-thumbnail.png");
  }

  @Nested
  @DisplayName("1. 영화(Movie) DTO -> Content 엔티티 매핑 검증")
  class MovieMappingTests {

    @Test
    @DisplayName("영화 DTO 변환 시 providerId, TMDB 프로바이더, MOVIE 타입 및 포스터 URL이 정상 매핑된다")
    void toEntity_success_whenValidMovieDtoProvided() {
      // given: 영화 DTO 구성 (title 비어있지 않음)
      TmdbContentDto dto =
          new TmdbContentDto(100L, "인셉션", null, "꿈 속 여행 이야기", "/inception.jpg", Set.of(28, 878));

      // when: toEntity 변환 수행 시
      Content content = tmdbContentMapper.toEntity(dto);

      // that: providerId, ContentProvider.TMDB, ContentType.MOVIE 및 썸네일 URL 매핑을 검증한다
      assertThat(content).isNotNull();
      assertThat(content.getProviderId()).isEqualTo("100");
      assertThat(content.getProvider()).isEqualTo(ContentProvider.TMDB);
      assertThat(content.getTitle()).isEqualTo("인셉션");
      assertThat(content.getType()).isEqualTo(ContentType.MOVIE);
      assertThat(content.getDescription()).isEqualTo("꿈 속 여행 이야기");
      assertThat(content.getThumbnailUrl())
          .isEqualTo("https://image.tmdb.org/t/p/w500/inception.jpg");
    }

    @Test
    @DisplayName("영화 설명 및 포스터 경로가 누락(null/blank)되면 디폴트 값이 적용된다")
    void toEntity_applyDefaultValues_whenDescriptionOrPosterIsNull() {
      // given: 설명과 포스터 경로가 null인 영화 DTO 구성
      TmdbContentDto dto = new TmdbContentDto(200L, "디폴트 영화", null, null, "   ", Set.of());

      // when: toEntity 변환 수행 시
      Content content = tmdbContentMapper.toEntity(dto);

      // that: 기본 설명 및 기본 썸네일 경로가 적용되었는지 검증한다
      assertThat(content.getDescription()).isEqualTo("설명이 없는 컨텐츠입니다.");
      assertThat(content.getThumbnailUrl()).isEqualTo("/uploads/default-thumbnail.png");
    }
  }

  @Nested
  @DisplayName("2. TV 시리즈(TV) DTO -> Content 엔티티 매핑 검증")
  class TvMappingTests {

    @Test
    @DisplayName("TV 시리즈 DTO(title은 null, name은 존재) 변환 시 TV_SERIES 타입과 resolveTitle이 정상 적용된다")
    void toEntity_success_whenValidTvDtoProvided() {
      // given: TV 시리즈 DTO 구성 (name 필드에 제목 존재)
      TmdbContentDto dto =
          new TmdbContentDto(300L, null, "오징어 게임", "글로벌 생존 게임", "/squid.jpg", Set.of(28));

      // when: toEntity 변환 수행 시
      Content content = tmdbContentMapper.toEntity(dto);

      // that: ContentType.TV_SERIES 및 제목 "오징어 게임"이 정상 매핑되었는지 검증한다
      assertThat(content).isNotNull();
      assertThat(content.getProviderId()).isEqualTo("300");
      assertThat(content.getTitle()).isEqualTo("오징어 게임");
      assertThat(content.getType()).isEqualTo(ContentType.TV_SERIES);
    }
  }
}
