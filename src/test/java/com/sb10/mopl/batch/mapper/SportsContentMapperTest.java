package com.sb10.mopl.batch.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.sb10.mopl.batch.dto.SportsContentDto;
import com.sb10.mopl.content.entity.Content;
import com.sb10.mopl.content.entity.ContentProvider;
import com.sb10.mopl.content.entity.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class SportsContentMapperTest {

  private SportsContentMapper sportsContentMapper;

  @BeforeEach
  void setUp() {
    sportsContentMapper = new SportsContentMapper();
    ReflectionTestUtils.setField(
        sportsContentMapper, "defaultImageUrl", "/uploads/default-thumbnail.png");
  }

  @Nested
  @DisplayName("1. SportsContentDto -> Content 엔티티 매핑 검증")
  class MappingTests {

    @Test
    @DisplayName("유효한 스포츠 DTO 변환 시 providerId, SPORTS_DB 프로바이더, SPORT 타입 및 제목이 정상 매핑된다")
    void toEntity_success_whenValidSportsContentDtoProvided() {
      // given: 유효한 SportsContentDto 구성
      SportsContentDto dto =
          new SportsContentDto(
              "EVT100", "토트넘 vs 아스널", "북런던 더비 하이라이트 경기 설명", "/thumb.jpg", "에미레이트 스타디움");

      // when: toEntity 변환 수행 시
      Content content = sportsContentMapper.toEntity(dto);

      // that: providerId, ContentProvider.SPORTS_DB, ContentType.SPORT 및 제목 매핑을 검증한다
      assertThat(content).isNotNull();
      assertThat(content.getProviderId()).isEqualTo("EVT100");
      assertThat(content.getProvider()).isEqualTo(ContentProvider.SPORTS_DB);
      assertThat(content.getTitle()).isEqualTo("토트넘 vs 아스널");
      assertThat(content.getType()).isEqualTo(ContentType.SPORT);
      assertThat(content.getDescription()).isEqualTo("북런던 더비 하이라이트 경기 설명");
      assertThat(content.getThumbnailUrl()).isEqualTo("/thumb.jpg");
    }

    @Test
    @DisplayName("스포츠 DTO의 썸네일/설명이 누락되면 기본 디폴트 값이 적용된다")
    void toEntity_applyDefaultValues_whenThumbnailOrDescriptionIsNull() {
      // given: 썸네일과 설명(strFilename)이 null인 SportsContentDto 구성
      SportsContentDto dto = new SportsContentDto("EVT200", "하이라이트 경기", null, null, "잠실 야구장");

      // when: toEntity 변환 수행 시
      Content content = sportsContentMapper.toEntity(dto);

      // that: 기본 설명 및 기본 썸네일 적용을 검증한다
      assertThat(content.getDescription()).isEqualTo("설명이 없는 스포츠 경기 콘텐츠입니다.");
      assertThat(content.getThumbnailUrl()).isEqualTo("/uploads/default-thumbnail.png");
    }
  }
}
