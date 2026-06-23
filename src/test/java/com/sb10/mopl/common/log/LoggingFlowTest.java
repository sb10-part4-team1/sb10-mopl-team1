package com.sb10.mopl.common.log;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sb10.mopl.config.MdcTaskDecorator;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** 로깅 및 MDC 트레이싱 핵심 기능들을 하나의 파일에서 구조화하여 검증하는 테스트 클래스입니다. */
class LoggingFlowTest {

  @Nested
  @DisplayName("LogMaskingUtils - 민감 정보 마스킹 및 인코딩 검증")
  class LogMaskingUtilsTest {

    @Test
    @DisplayName("Query Parameter 내의 민감한 정보(비밀번호, 토큰 등)를 마스킹 처리")
    void maskParameters_returnMaskedQueryString_whenQueryContainsSensitiveKeys() {
      // given
      String query = "username=admin&password=secretPassword&token=myToken";

      // when
      String masked = LogMaskingUtils.maskParameters(query);

      // then
      assertEquals("username=admin&password=******&token=******", masked);
    }

    @Test
    @DisplayName("JSON 바디 내 문자열 형태의 민감 정보(비밀번호, 토큰 등)를 마스킹 처리")
    void getMaskedBody_returnMaskedJson_whenJsonContainsSensitiveStrings() {
      // given
      String json =
          "{\"username\":\"admin\",\"password\":\"myPassword123\",\"accessToken\":\"secretToken\"}";

      // when
      String masked = LogMaskingUtils.getMaskedBody(json.getBytes(), "UTF-8");

      // then
      assertTrue(masked.contains("\"password\":\"******\""));
      assertTrue(masked.contains("\"accessToken\":\"******\""));
      assertTrue(masked.contains("\"username\":\"admin\""));
    }

    @Test
    @DisplayName("JSON 바디 내 숫자, 불리언, null 형태의 민감 정보를 마스킹 처리")
    void getMaskedBody_returnMaskedJson_whenJsonContainsSensitivePrimitives() {
      // given
      String json = "{\"username\":\"admin\",\"phone\":123456789,\"secret\":true,\"token\":null}";

      // when
      String masked = LogMaskingUtils.getMaskedBody(json.getBytes(), "UTF-8");

      // then
      assertTrue(masked.contains("\"phone\":\"******\""));
      assertTrue(masked.contains("\"secret\":\"******\""));
      assertTrue(masked.contains("\"token\":\"******\""));
      assertTrue(masked.contains("\"username\":\"admin\""));
    }

    @Test
    @DisplayName("Form URL Encoded 본문 내의 민감한 매개변수를 마스킹 처리")
    void getMaskedBody_returnMaskedForm_whenFormContainsSensitiveKeys() {
      // given
      String form = "username=admin&password=myPassword123&refreshToken=myRefreshToken";

      // when
      String masked = LogMaskingUtils.getMaskedBody(form.getBytes(), "UTF-8");

      // then
      assertEquals("username=admin&password=******&refreshToken=******", masked);
    }

    @Test
    @DisplayName("인코딩이 ISO-8859-1로 제공되더라도 한글 깨짐을 방지하기 위해 UTF-8로 디코딩을 수행")
    void getMaskedBody_returnDecodedUtf8_whenResponseIsIso88591() {
      // given
      String koreanText = "한글 텍스트";

      // when
      String body = LogMaskingUtils.getMaskedBody(koreanText.getBytes(), "ISO-8859-1");

      // then
      assertEquals(koreanText, body);
    }

    @Test
    @DisplayName("HTTP 헤더 중 Authorization과 같은 민감한 필드를 마스킹 처리")
    void getMaskedHeaders_returnMaskedHeaders_whenHeadersAreSensitive() {
      // given
      HttpServletRequest request = mock(HttpServletRequest.class);
      when(request.getHeaderNames())
          .thenReturn(Collections.enumeration(Collections.singletonList("Authorization")));
      when(request.getHeader("Authorization")).thenReturn("Bearer mySecretToken");

      // when
      Map<String, String> maskedHeaders = LogMaskingUtils.getMaskedHeaders(request);

      // then
      assertEquals("Bearer ******", maskedHeaders.get("Authorization"));
    }

    @Test
    @DisplayName("본문이 최대 길이를 초과하는 경우, 민감 정보 부분 유출을 방지하기 위해 마스킹을 먼저 수행한 후 자름")
    void getMaskedBody_returnMaskedThenTruncated_whenBodyExceedsMaxLength() {
      // given
      String spaces = " ".repeat(1005);
      String json = spaces + "{\"password\":\"my SecretPassword\"}";

      // when
      String masked = LogMaskingUtils.getMaskedBody(json.getBytes(), "UTF-8");

      // then
      assertTrue(masked.contains("\"password\":\"******"));
      assertFalse(masked.contains("my"));
      assertFalse(masked.contains("Sec"));
      assertFalse(masked.contains("SecretPassword"));
    }
  }

  @Nested
  @DisplayName("ClientIpExtractor - HTTP 요청 헤더로부터 IP 추출 검증")
  class ClientIpExtractorTest {

    @Test
    @DisplayName("X-Forwarded-For 헤더가 있을 때 첫 번째 IP를 올바르게 추출")
    void getClientIp_returnFirstIp_whenProxyHeaderExists() {
      // given
      HttpServletRequest request = mock(HttpServletRequest.class);
      when(request.getHeader("X-Forwarded-For"))
          .thenReturn("203.0.113.195, 70.41.3.18, 150.172.238.178");

      // when
      String clientIp = ClientIpExtractor.getClientIp(request);

      // then
      assertEquals("203.0.113.195", clientIp);
    }

    @Test
    @DisplayName("프록시 헤더가 없을 때 request.getRemoteAddr() 값을 반환")
    void getClientIp_returnRemoteAddr_whenNoProxyHeadersExist() {
      // given
      HttpServletRequest request = mock(HttpServletRequest.class);
      when(request.getRemoteAddr()).thenReturn("192.168.1.50");

      // when
      String clientIp = ClientIpExtractor.getClientIp(request);

      // then
      assertEquals("192.168.1.50", clientIp);
    }
  }

  @Nested
  @DisplayName("MdcTaskDecorator - 비동기 스레드로 MDC 전파 검증")
  class MdcTaskDecoratorTest {

    @Test
    @DisplayName("TaskDecorator를 통해 MDC 컨텍스트가 자식 스레드로 올바르게 전파되는지 검증")
    void decorate_propagateMdcContext_whenTaskIsExecuted() throws InterruptedException {
      // given
      MDC.put("traceId", "test-trace-id");
      MDC.put("clientIp", "127.0.0.1");

      MdcTaskDecorator decorator = new MdcTaskDecorator();
      AtomicReference<String> childTraceId = new AtomicReference<>();
      AtomicReference<String> childClientIp = new AtomicReference<>();
      CountDownLatch latch = new CountDownLatch(1);

      Runnable task =
          () -> {
            childTraceId.set(MDC.get("traceId"));
            childClientIp.set(MDC.get("clientIp"));
            latch.countDown();
          };

      Runnable decoratedTask = decorator.decorate(task);

      MDC.clear();

      // when
      Thread childThread = new Thread(decoratedTask);
      childThread.start();
      latch.await(2, TimeUnit.SECONDS);

      // then
      assertEquals("test-trace-id", childTraceId.get());
      assertEquals("127.0.0.1", childClientIp.get());
    }
  }

  @Nested
  @DisplayName("RequestLoggingFilter - 요청 및 응답 로깅 흐름(동기/비동기) 검증")
  class RequestLoggingFilterTest {

    @Test
    @DisplayName("동기식 HTTP 요청에 대해 필터 통과 시 즉시 요청/응답이 로깅되고 바디가 복사되는지 검증")
    void doFilterInternal_logImmediately_whenRequestIsSynchronous() throws Exception {
      // given
      MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/test");
      request.setContentType("application/json");
      request.setContent("{\"key\":\"value\"}".getBytes());

      MockHttpServletResponse response = new MockHttpServletResponse();
      response.setContentType("application/json");

      FilterChain filterChain =
          (req, res) -> {
            res.setContentType("application/json");
            res.getWriter().write("{\"status\":\"ok\"}");
          };

      RequestLoggingFilter filter = new RequestLoggingFilter();

      // when
      filter.doFilter(request, response, filterChain);

      // then
      assertEquals("{\"status\":\"ok\"}", response.getContentAsString());
    }

    @Test
    @DisplayName("비동기식 HTTP 요청에 대해 필터 통과 시 즉시 로깅/복사되지 않고 AsyncListener 완료 시점에 로깅 및 바디 복사가 일어나는지 검증")
    void doFilterInternal_deferLogAndCopy_whenRequestIsAsynchronous() throws Exception {
      // given
      MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/async-test");
      request.setContentType("application/json");
      request.setContent("{\"key\":\"value\"}".getBytes());
      request.setAsyncSupported(true);
      request.startAsync(); // 비동기 실행 및 AsyncContext 초기화

      MockHttpServletResponse response = new MockHttpServletResponse();
      response.setContentType("application/json");

      FilterChain filterChain =
          (req, res) -> {
            res.setContentType("application/json");
            res.getWriter().write("{\"status\":\"ok\"}");
          };

      RequestLoggingFilter filter = new RequestLoggingFilter();

      // when (비동기 서블릿 최초 디스패치 실행)
      filter.doFilter(request, response, filterChain);

      // then: 완료 전이므로 원래 응답 객체에는 아직 바디가 복사되지 않은 빈 상태여야 함
      assertEquals("", response.getContentAsString());

      // when: 비동기 작업 완료(onComplete) 이벤트 시뮬레이션
      org.springframework.mock.web.MockAsyncContext asyncContext =
          (org.springframework.mock.web.MockAsyncContext) request.getAsyncContext();
      assertNotNull(asyncContext);

      // 등록된 리스너를 가져와 완료 이벤트 전송
      AsyncListener listener = asyncContext.getListeners().get(0);
      listener.onComplete(new AsyncEvent(asyncContext, request, response));

      // then: 완료 이벤트 수신 후 최종적으로 응답 바디가 제대로 복사되었는지 확인
      assertEquals("{\"status\":\"ok\"}", response.getContentAsString());
    }
  }
}
