package com.sb10.mopl.common.log;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

/*
 * HTTP 요청 및 응답 본문 캐싱을 통해 페이로드 정보를 마스킹 처리하여 기록하는 로깅 필터입니다.
 * MdcFilter 바로 다음 순서(Highest Precedence +1)로 실행됩니다.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RequestLoggingFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    // =========================================================================
    // [실시간 알림 SSE 채널 캐싱 우회 (Bypass)]
    // SSE(Server-Sent Events) 스트리밍 채널은 연결이 끊어지지 않고 계속 데이터를 밀어 보냅니다.
    // 여기에 응답 본문 캐싱 래퍼(ContentCachingResponseWrapper)를 씌워버리면
    // 데이터가 즉시 방출되지 않고 메모리 버퍼에 묶여 톰캣 스레드가 통째로 마비되는 장애가 터집니다.
    // 이를 원천 방지하기 위해 /api/sse 요청은 캐싱 처리를 생략하고 즉시 통과시킵니다.
    // =========================================================================
    String uri = request.getRequestURI();
    if (uri.equals("/api/sse") || uri.startsWith("/api/sse/")) {
      filterChain.doFilter(request, response);
      return;
    }

    ContentCachingRequestWrapper requestWrapper =
        new ContentCachingRequestWrapper(request, 1024 * 1024);
    ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

    long startTime = System.currentTimeMillis();

    try {
      filterChain.doFilter(requestWrapper, responseWrapper);
    } finally {
      if (request.isAsyncStarted()) {
        request
            .getAsyncContext()
            .addListener(
                new LoggingAsyncListener(
                    requestWrapper, responseWrapper, startTime, MDC.getCopyOfContextMap(), this));
      } else {
        logAndCopyResponse(requestWrapper, responseWrapper, startTime);
      }
    }
  }

  /** 동기식 혹은 비동기식 완료 시점에 요청/응답 정보 로깅 및 바디 복사를 통합 처리하는 헬퍼 메서드입니다. */
  void logAndCopyResponse(
      ContentCachingRequestWrapper request,
      ContentCachingResponseWrapper response,
      long startTime) {
    long duration = System.currentTimeMillis() - startTime;
    try {
      logRequestAndResponse(request, response, duration);
    } catch (Exception e) {
      log.warn("요청/응답 로깅 실패", e);
    } finally {
      try {
        response.copyBodyToResponse();
      } catch (IOException e) {
        log.warn("응답 바디 복사 실패", e);
      }
    }
  }

  /** 요청과 응답 정보를 가공하고 마스킹하여 로그를 남깁니다. */
  private void logRequestAndResponse(
      ContentCachingRequestWrapper request, ContentCachingResponseWrapper response, long duration) {

    // URI에 쿼리 파라미터 결합 (Logback이 마스킹을 위임받음)
    String fullUri =
        request.getRequestURI()
            + (request.getQueryString() != null ? "?" + request.getQueryString() : "");

    String requestParams = request.getParameterMap().toString();
    String requestBody =
        getBodyIfLoggable(
            request.getContentAsByteArray(),
            request.getContentType(),
            request.getCharacterEncoding());

    log.info(
        "Request: Method=[{}], URI=[{}], Params={}, Body=[{}]",
        request.getMethod(),
        fullUri,
        requestParams,
        requestBody);

    byte[] responseContent = response.getContentAsByteArray();
    String responseBody =
        getBodyIfLoggableForResponse(
            responseContent, response.getContentType(), response.getCharacterEncoding());

    log.info(
        "Response: Status=[{}], Duration=[{}ms], Body=[{}]",
        response.getStatus(),
        duration,
        responseBody);
  }

  /**
   * 로깅 가능한 텍스트 콘텐츠 타입인 경우에만 바디 문자열을 추출합니다. Content-Length 헤더에 의존하지 않고 실제 캐싱된 바이트 데이터의 길이만 기준으로
   * 판단합니다.
   */
  private String getBodyIfLoggable(byte[] content, String contentType, String encoding) {
    if (content == null || content.length == 0) {
      return "";
    }
    if (!isLoggableContentType(contentType)) {
      return "[Payload Skipped]";
    }
    try {
      String charEncoding =
          encoding == null || "ISO-8859-1".equalsIgnoreCase(encoding) ? "UTF-8" : encoding;
      String body = new String(content, charEncoding);
      if (body.length() > 1024) {
        return body.substring(0, 1024) + "... (truncated)";
      }
      return body;
    } catch (Exception e) {
      return "[Payload Encoding Error]";
    }
  }

  /** 응답 바디 로깅 헬퍼 메서드입니다. 1KB(1024바이트)를 초과하는 대용량 응답은 성능 최적화를 위해 로깅을 건너뜁니다. */
  private String getBodyIfLoggableForResponse(byte[] content, String contentType, String encoding) {
    if (content == null || content.length == 0) {
      return "";
    }
    if (!isLoggableContentType(contentType)) {
      return "[Payload Skipped]";
    }
    // 대용량 응답(1KB 초과)은 톰캣 성능 지연을 방지하기 위해 로깅 생략
    if (content.length > 1024) {
      return "[Payload Skipped] (over 1KB)";
    }
    try {
      String charEncoding =
          encoding == null || "ISO-8859-1".equalsIgnoreCase(encoding) ? "UTF-8" : encoding;
      return new String(content, charEncoding);
    } catch (Exception e) {
      return "[Payload Encoding Error]";
    }
  }

  /** 본문 데이터를 로깅할 수 있는 텍스트 기반 Content-Type인지 확인합니다. */
  private boolean isLoggableContentType(String contentType) {
    if (contentType == null) {
      return false;
    }
    String lower = contentType.toLowerCase();
    return lower.contains("application/json")
        || lower.contains("+json")
        || lower.contains("application/xml")
        || lower.contains("application/x-www-form-urlencoded")
        || lower.contains("text/plain")
        || lower.contains("text/xml");
  }
}
