package com.sb10.mopl.batch.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.sb10.mopl.batch.dto.SportsApiResponse;
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

class SportsApiClientTest {

  private RestClient restClient;
  private MockRestServiceServer mockServer;
  private SportsApiClient sportsApiClient;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder =
        RestClient.builder().baseUrl("https://www.thesportsdb.com/api/v1/json/test");
    mockServer = MockRestServiceServer.bindTo(builder).build();
    restClient = builder.build();
    sportsApiClient = new SportsApiClient(restClient);
  }

  @Nested
  @DisplayName("SportsApiClient 스포츠 데이터 수집 정상 성공 검증")
  class SuccessCases {

    @Test
    @DisplayName("정상 API 응답(JSON)이 올 경우 SportsApiResponse 객체로 성공적으로 파싱되어 반환된다")
    void fetchEventsByDay_success_whenValidJsonResponseReturned() {
      // given: Mock REST 응답 JSON 준비
      String jsonResponse = "{\"events\":[]}";
      mockServer
          .expect(
              requestTo(
                  "https://www.thesportsdb.com/api/v1/json/test/eventsday.php?d=2026-07-27&l=4328"))
          .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

      // when: fetchEventsByDay 메서드 호출 시
      SportsApiResponse response = sportsApiClient.fetchEventsByDay("2026-07-27", 4328);

      // that: 응답 객체가 정상 반환되는지 검증한다
      assertThat(response).isNotNull();
      assertThat(response.getEvents()).isEmpty();
      mockServer.verify();
    }
  }

  @Nested
  @DisplayName("SportsApiClient 응답 누락 및 외부 예외 전파 검증")
  class ExceptionCases {

    @Test
    @DisplayName("API 응답 바디가 누락되어 null이 리턴될 경우 INVALID_API_RESPONSE 예외가 발생한다")
    void fetchEventsByDay_fail_whenResponseBodyIsNull() {
      // given: 빈 200 OK 응답(바디 없음) 설정
      mockServer
          .expect(
              requestTo(
                  "https://www.thesportsdb.com/api/v1/json/test/eventsday.php?d=2026-07-27&l=4328"))
          .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

      // when & that: fetchEventsByDay 호출 시 BatchException(INVALID_API_RESPONSE) 발생하는지 검증한다
      assertThatThrownBy(() -> sportsApiClient.fetchEventsByDay("2026-07-27", 4328))
          .isInstanceOf(BatchException.class)
          .extracting("errorCode")
          .isEqualTo(BatchErrorCode.INVALID_API_RESPONSE);

      mockServer.verify();
    }

    @Test
    @DisplayName("외부 SportsDB 서버 5xx 에러 발생 시 HttpServerErrorException이 상위로 전파된다")
    void fetchEventsByDay_fail_whenHttpServerErrorOccurs() {
      // given: 외부 서버 500 Internal Server Error 응답 설정
      mockServer
          .expect(
              requestTo(
                  "https://www.thesportsdb.com/api/v1/json/test/eventsday.php?d=2026-07-27&l=4328"))
          .andRespond(withServerError());

      // when & that: fetchEventsByDay 호출 시 HttpServerErrorException이 상위로 전파되는지 검증한다
      assertThatThrownBy(() -> sportsApiClient.fetchEventsByDay("2026-07-27", 4328))
          .isInstanceOf(HttpServerErrorException.class);

      mockServer.verify();
    }

    @Test
    @DisplayName("네트워크 장애로 타임아웃 발생 시 ResourceAccessException이 상위로 전파된다")
    void fetchEventsByDay_fail_whenResourceAccessExceptionOccurs() {
      // given: 네트워크 타임아웃 예외를 유발하는 mockServer 응답 설정
      mockServer
          .expect(
              requestTo(
                  "https://www.thesportsdb.com/api/v1/json/test/eventsday.php?d=2026-07-27&l=4328"))
          .andRespond(
              request -> {
                throw new ResourceAccessException("연결 타임아웃 발생");
              });

      // when & that: fetchEventsByDay 호출 시 ResourceAccessException이 상위로 전파되는지 검증한다
      assertThatThrownBy(() -> sportsApiClient.fetchEventsByDay("2026-07-27", 4328))
          .isInstanceOf(ResourceAccessException.class);

      mockServer.verify();
    }
  }
}
