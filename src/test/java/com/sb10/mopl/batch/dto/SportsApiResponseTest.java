package com.sb10.mopl.batch.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SportsApiResponseTest {

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
  }

  @Nested
  @DisplayName("1. SportsDB API JSON 응답 문자열 -> DTO 역직렬화(Deserialization) 검증")
  class JsonParsingTests {

    @Test
    @DisplayName("SportsDB API 응답 JSON 문자열이 SportsApiResponse 및 SportsContentDto 필드로 정확히 파싱된다")
    void deserialize_success_whenValidJsonStringProvided() throws Exception {
      // given: SportsDB API 응답 규격 JSON 샘플
      String json =
          """
          {
            "events": [
              {
                "idEvent": "EVT12345",
                "strEvent": "토트넘 vs 아스널",
                "strFilename": "북런던 더비 하이라이트",
                "strThumb": "/thumb_photo.jpg",
                "strVenue": "에미레이트 스타디움"
              }
            ]
          }
          """;

      // when: ObjectMapper를 통해 DTO 역직렬화 수행 시
      SportsApiResponse response = objectMapper.readValue(json, SportsApiResponse.class);

      // that: events 목록 및 DTO 필드 정상 파싱을 검증한다
      assertThat(response).isNotNull();
      assertThat(response.getEvents()).hasSize(1);

      SportsContentDto contentDto = response.getEvents().get(0);
      assertThat(contentDto.idEvent()).isEqualTo("EVT12345");
      assertThat(contentDto.strEvent()).isEqualTo("토트넘 vs 아스널");
      assertThat(contentDto.strFilename()).isEqualTo("북런던 더비 하이라이트");
      assertThat(contentDto.strThumb()).isEqualTo("/thumb_photo.jpg");
      assertThat(contentDto.strVenue()).isEqualTo("에미레이트 스타디움");
    }
  }
}
