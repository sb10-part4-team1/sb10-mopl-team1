package com.sb10.mopl.batch.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sb10.mopl.batch.dto.TmdbContentDto;
import com.sb10.mopl.batch.mapper.TmdbContentMapper;
import com.sb10.mopl.content.entity.Content;
import com.sb10.mopl.content.entity.ContentType;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TmdbItemProcessorTest {

  @Mock private TmdbContentMapper tmdbContentMapper;

  @InjectMocks private TmdbItemProcessor tmdbItemProcessor;

  @Test
  @DisplayName("정상적인 DTO 정보가 유입되면 검증을 통과하고 Content 엔티티로 변환에 성공한다")
  void process_success_whenDataIsValid() {
    // given: 정상적인 TMDB DTO 데이터와 매퍼 모킹 설정
    TmdbContentDto dto =
        new TmdbContentDto(100L, "인셉션", null, "SF 영화", "/inception.jpg", List.of(1));
    Content content = Content.create("인셉션", ContentType.MOVIE, "SF 영화", "/inception.jpg");
    when(tmdbContentMapper.toEntity(dto)).thenReturn(content);

    // when: 프로세서 호출
    Content result = tmdbItemProcessor.process(dto);

    // then: 스킵되지 않고 알맞게 변환된 Entity가 반환되는지 확인
    assertThat(result).isNotNull();
    assertThat(result.getTitle()).isEqualTo("인셉션");
    verify(tmdbContentMapper).toEntity(dto);
  }

  @Test
  @DisplayName("DTO의 제목정보가 누락되면 null을 반환하여 해당 아이템을 스킵한다")
  void process_skip_whenTitleIsMissing() {
    // given: 제목이 null인 DTO 구성
    TmdbContentDto dto = new TmdbContentDto(100L, null, null, "설명은 있음", "/path.jpg", List.of(1));

    // when: 프로세서 호출
    Content result = tmdbItemProcessor.process(dto);

    // then: null이 반환되어 최종 라이터 전 단계에서 스킵되는지 확인
    assertThat(result).isNull();
  }

  @Test
  @DisplayName("DTO의 TMDB ID(provider_id)가 누락되면 null을 반환하여 해당 아이템을 스킵한다")
  void process_skip_whenIdIsMissing() {
    // given: ID가 null인 DTO 구성
    TmdbContentDto dto =
        new TmdbContentDto(null, "제목은 있음", null, "설명도 있음", "/path.jpg", List.of(1));

    // when: 프로세서 호출
    Content result = tmdbItemProcessor.process(dto);

    // then: null이 반환되어 최종 라이터 전 단계에서 스킵되는지 확인
    assertThat(result).isNull();
  }
}
