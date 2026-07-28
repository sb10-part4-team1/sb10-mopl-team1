package com.sb10.mopl.batch.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.sb10.mopl.batch.dto.TmdbApiResponse;
import com.sb10.mopl.batch.exception.BatchErrorCode;
import com.sb10.mopl.batch.exception.BatchException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

class TmdbApiClientTest {

  private RestClient restClient;
  private MockRestServiceServer mockServer;
  private TmdbApiClient tmdbApiClient;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder().baseUrl("https://api.themoviedb.org/3");
    mockServer = MockRestServiceServer.bindTo(builder).build();
    restClient = builder.build();
    tmdbApiClient = new TmdbApiClient(restClient);
  }

  @Nested
  @DisplayName("TmdbApiClient 데이터 수집 정상 성공 검증")
  class SuccessCases {

    @Test
    @DisplayName("정상 API 응답(JSON)이 올 경우 DTO 객체로 성공적으로 파싱되어 반환된다")
    void fetch_success_whenValidJsonResponseReturned() {
      // given: Mock REST 응답 JSON 준비
      String jsonResponse = "{\"page\":1,\"results\":[],\"total_pages\":1,\"total_results\":0}";
      mockServer
          .expect(requestTo("https://api.themoviedb.org/3/movie/popular?language=ko-KR&page=1"))
          .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

      // when: fetch 메서드 호출 시
      TmdbApiResponse response = tmdbApiClient.fetch("/movie/popular", 1, TmdbApiResponse.class);

      // that: 응답 객체가 정상적으로 파싱되었는지 검증한다
      assertThat(response).isNotNull();
      assertThat(response.getPage()).isEqualTo(1);
      mockServer.verify();
    }
  }

  @Nested
  @DisplayName("TmdbApiClient 응답 누락 및 외부 예외 전파 검증")
  class ExceptionCases {

    @Test
    @DisplayName("API 응답 바디가 누락되어 null이 리턴될 경우 INVALID_API_RESPONSE 예외가 발생한다")
    void fetch_fail_whenResponseBodyIsNull() {
      // given: 빈 200 OK 응답(바디 없음) 설정
      mockServer
          .expect(requestTo("https://api.themoviedb.org/3/movie/popular?language=ko-KR&page=1"))
          .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

      // when & that: fetch 호출 시 BatchException(INVALID_API_RESPONSE) 발생하는지 검증한다
      assertThatThrownBy(() -> tmdbApiClient.fetch("/movie/popular", 1, TmdbApiResponse.class))
          .isInstanceOf(BatchException.class)
          .extracting("errorCode")
          .isEqualTo(BatchErrorCode.INVALID_API_RESPONSE);

      mockServer.verify();
    }

    @Test
    @DisplayName("외부 TMDB 서버 5xx 에러 발생 시 HttpServerErrorException이 상위로 전파된다")
    void fetch_fail_whenHttpServerErrorOccurs() {
      // given: 외부 서버 500 Internal Server Error 응답 설정
      mockServer
          .expect(requestTo("https://api.themoviedb.org/3/movie/popular?language=ko-KR&page=1"))
          .andRespond(withServerError());

      // when & that: fetch 호출 시 HttpServerErrorException이 상위로 전파되는지 검증한다
      assertThatThrownBy(() -> tmdbApiClient.fetch("/movie/popular", 1, TmdbApiResponse.class))
          .isInstanceOf(HttpServerErrorException.class);

      mockServer.verify();
    }

    @Test
    @DisplayName("네트워크 장애로 타임아웃 발생 시 ResourceAccessException이 상위로 전파된다")
    void fetch_fail_whenResourceAccessExceptionOccurs() {
      // given: 네트워크 타임아웃 예외를 유발하는 mockServer 응답 설정
      mockServer
          .expect(requestTo("https://api.themoviedb.org/3/movie/popular?language=ko-KR&page=1"))
          .andRespond(
              request -> {
                throw new ResourceAccessException("연결 타임아웃 발생");
              });

      // when & that: fetch 호출 시 ResourceAccessException이 상위로 전파되는지 검증한다
      assertThatThrownBy(() -> tmdbApiClient.fetch("/movie/popular", 1, TmdbApiResponse.class))
          .isInstanceOf(ResourceAccessException.class);

      mockServer.verify();
    }
  }
}
