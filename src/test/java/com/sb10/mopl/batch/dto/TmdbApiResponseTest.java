package com.sb10.mopl.batch.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TmdbApiResponseTest {

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
  }

  @Nested
  @DisplayName("1. TMDB API JSON 응답 문자열 -> DTO 역직렬화(Deserialization) 검증")
  class JsonParsingTests {

    @Test
    @DisplayName("TMDB API 응답 JSON 문자열이 TmdbApiResponse 및 TmdbContentDto의 필드로 정확히 파싱된다")
    void deserialize_success_whenValidJsonStringProvided() throws Exception {
      // given: TMDB API 실제 응답 규격의 JSON 샘플
      String json =
          """
          {
            "page": 1,
            "total_pages": 5,
            "total_results": 100,
            "results": [
              {
                "id": 550,
                "title": "파이트 클럽",
                "name": null,
                "overview": "규칙 1: 파이트 클럽에 대해 말하지 마라.",
                "poster_path": "/fight_club.jpg",
                "genre_ids": [18, 28]
              }
            ]
          }
          """;

      // when: ObjectMapper를 통해 DTO 역직렬화 수행 시
      TmdbApiResponse response = objectMapper.readValue(json, TmdbApiResponse.class);

      // that: page, totalPages 및 results 내부 DTO 필드가 정확히 파싱되었는지 검증한다
      assertThat(response).isNotNull();
      assertThat(response.getPage()).isEqualTo(1);
      assertThat(response.getTotalPages()).isEqualTo(5);
      assertThat(response.getResults()).hasSize(1);

      TmdbContentDto contentDto = response.getResults().get(0);
      assertThat(contentDto.id()).isEqualTo(550L);
      assertThat(contentDto.title()).isEqualTo("파이트 클럽");
      assertThat(contentDto.overview()).isEqualTo("규칙 1: 파이트 클럽에 대해 말하지 마라.");
      assertThat(contentDto.posterPath()).isEqualTo("/fight_club.jpg");
      assertThat(contentDto.genreIds()).containsExactlyInAnyOrder(18, 28);
    }
  }
}
