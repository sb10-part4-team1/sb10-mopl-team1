package com.sb10.mopl.batch.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sb10.mopl.batch.dto.SportsContentDto;
import com.sb10.mopl.batch.mapper.SportsContentMapper;
import com.sb10.mopl.content.entity.Content;
import com.sb10.mopl.content.entity.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SportsItemProcessorTest {

  @Mock private SportsContentMapper sportsContentMapper;

  @InjectMocks private SportsItemProcessor sportsItemProcessor;

  @Test
  @DisplayName("정상적인 DTO 정보가 유입되면 검증을 통과하고 Content 엔티티로 변환에 성공한다")
  void process_success_whenDataIsValid() {
    // given: 정상적인 Sports DTO 데이터와 매퍼 모킹 설정
    SportsContentDto dto =
        new SportsContentDto(
            "2267443", "Brighton vs Man United", "EPL 2026-05-24", "/path.jpg", "Stadium");
    Content content =
        Content.create("Brighton vs Man United", ContentType.SPORT, "EPL 2026-05-24", "/path.jpg");
    when(sportsContentMapper.toEntity(dto)).thenReturn(content);

    // when: 프로세서 호출
    Content result = sportsItemProcessor.process(dto);

    // then: 스킵되지 않고 알맞게 변환된 Entity가 반환되는지 확인
    assertThat(result).isNotNull();
    assertThat(result.getTitle()).isEqualTo("Brighton vs Man United");
    verify(sportsContentMapper).toEntity(dto);
  }

  @Test
  @DisplayName("DTO의 strEvent(title)가 누락되면 null을 반환하여 해당 아이템을 스킵한다")
  void process_skip_whenTitleIsMissing() {
    // given: 제목이 null인 DTO 구성
    SportsContentDto dto =
        new SportsContentDto("2267443", null, "EPL 2026-05-24", "/path.jpg", "Stadium");

    // when: 프로세서 호출
    Content result = sportsItemProcessor.process(dto);

    // then: null이 반환되어 최종 라이터 전 단계에서 스킵되는지 확인
    assertThat(result).isNull();
  }

  @Test
  @DisplayName("DTO의 idEvent(provider_id)가 누락되면 null을 반환하여 해당 아이템을 스킵한다")
  void process_skip_whenIdIsMissing() {
    // given: 식별자 ID가 null인 DTO 구성
    SportsContentDto dto =
        new SportsContentDto(
            null, "Brighton vs Man United", "EPL 2026-05-24", "/path.jpg", "Stadium");

    // when: 프로세서 호출
    Content result = sportsItemProcessor.process(dto);

    // then: null이 반환되어 최종 라이터 전 단계에서 스킵되는지 확인
    assertThat(result).isNull();
  }
}
