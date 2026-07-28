package com.sb10.mopl.batch.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.sb10.mopl.batch.dto.TmdbApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.retry.backoff.Sleeper;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

@SpringBootTest
@ActiveProfiles("test")
@Import(ApiClientIntegrationTest.NoSleepRetryConfig.class)
class ApiClientIntegrationTest {

  @TestConfiguration
  static class NoSleepRetryConfig {
    @Bean
    public Sleeper sleeper() {
      // Spring Retry의 Thread.sleep 대기를 0ms로 무력화하는 No-Op Sleeper
      return backOffPeriod -> {};
    }
  }

  @Autowired private TmdbApiClient tmdbApiClient;

  private MockRestServiceServer mockServer;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder().baseUrl("https://api.themoviedb.org/3");
    mockServer = MockRestServiceServer.bindTo(builder).build();
    RestClient mockRestClient = builder.build();
    ReflectionTestUtils.setField(tmdbApiClient, "restClient", mockRestClient);
  }

  @Nested
  @DisplayName("1. Spring Retry (@Retryable) 메커니즘 및 API Client 통합 검증")
  class RetryAndParsingTests {

    @Test
    @DisplayName("500 서버 에러 발생 시 지정된 maxAttempts 횟수만큼 재시도를 수행한 후 최종 예외가 던져진다")
    void fetch_retriesMaxAttempts_whenHttpServerErrorOccurs() {
      // given: 4회의 연속 500 에러 응답을 하도록 Mock REST 서버 구성 (maxAttempts = 4)
      for (int i = 0; i < 4; i++) {
        mockServer
            .expect(requestTo("https://api.themoviedb.org/3/movie/popular?language=ko-KR&page=1"))
            .andRespond(withServerError());
      }

      // when & that: fetch 호출 시 Spring Retry에 의해 재시도가 수행되고 최종 HttpServerErrorException이 전파되는지 검증한다
      assertThatThrownBy(() -> tmdbApiClient.fetch("/movie/popular", 1, TmdbApiResponse.class))
          .isInstanceOf(HttpServerErrorException.class);

      mockServer.verify();
    }

    @Test
    @DisplayName("초기 1회 500 에러 발생 후 재시도 시 정상 응답이 올 경우 재시도를 거쳐 DTO 파싱에 성공한다")
    void fetch_successAfterRetry_whenServerRecovers() {
      // given: 첫 번째 요청은 500 에러, 두 번째 요청은 정상 200 OK 응답하도록 설정
      String validJson = "{\"page\":1,\"results\":[],\"total_pages\":1,\"total_results\":0}";

      mockServer
          .expect(requestTo("https://api.themoviedb.org/3/movie/popular?language=ko-KR&page=1"))
          .andRespond(withServerError());

      mockServer
          .expect(requestTo("https://api.themoviedb.org/3/movie/popular?language=ko-KR&page=1"))
          .andRespond(withSuccess(validJson, MediaType.APPLICATION_JSON));

      // when: fetch 호출 시
      TmdbApiResponse response = tmdbApiClient.fetch("/movie/popular", 1, TmdbApiResponse.class);

      // that: 1회 실패 후 2회차 재시도에 성공하여 DTO 객체가 파싱되었는지 검증한다
      assertThat(response).isNotNull();
      assertThat(response.getPage()).isEqualTo(1);
      mockServer.verify();
    }
  }
}
