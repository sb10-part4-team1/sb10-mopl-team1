package com.sb10.mopl.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * HTTP 요청 및 응답 본문 캐싱을 통해 페이로드 정보를 마스킹 처리하여 기록하는 로깅 필터입니다. MdcFilter 바로 다음 순서(Highest Precedence +
 * 1)로 실행됩니다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RequestLoggingFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    ContentCachingRequestWrapper requestWrapper =
        new ContentCachingRequestWrapper(request, 1024 * 1024);
    ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

    long startTime = System.currentTimeMillis();

    try {
      filterChain.doFilter(requestWrapper, responseWrapper);
    } finally {
      long duration = System.currentTimeMillis() - startTime;
      logRequestAndResponse(requestWrapper, responseWrapper, duration);
      responseWrapper.copyBodyToResponse();
    }
  }

  /** 요청과 응답 정보를 가공하고 마스킹하여 로그를 남깁니다. */
  private void logRequestAndResponse(
      ContentCachingRequestWrapper request, ContentCachingResponseWrapper response, long duration) {

    // URI에 마스킹된 쿼리 파라미터 결합
    String fullUri =
        request.getRequestURI()
            + (request.getQueryString() != null
                ? "?" + LogMaskingUtils.maskParameters(request.getQueryString())
                : "");

    String requestParams = LogMaskingUtils.getMaskedRequestParams(request);
    String requestBody =
        getBodyIfLoggable(
            request.getContentAsByteArray(),
            request.getContentLength(),
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
        getBodyIfLoggable(
            responseContent,
            responseContent.length,
            response.getContentType(),
            response.getCharacterEncoding());

    log.info(
        "Response: Status=[{}], Duration=[{}ms], Body=[{}]",
        response.getStatus(),
        duration,
        responseBody);
  }

  /** 로깅 가능한 텍스트 콘텐츠 타입인 경우에만 마스킹된 바디 문자열을 추출합니다. */
  private String getBodyIfLoggable(
      byte[] content, int contentLength, String contentType, String encoding) {
    if (contentLength <= 0 || content.length == 0) {
      return "";
    }
    return isLoggableContentType(contentType)
        ? LogMaskingUtils.getMaskedBody(content, encoding)
        : "[Payload Skipped]";
  }

  /** 본문 데이터를 로깅할 수 있는 텍스트 기반 Content-Type인지 확인합니다. */
  private boolean isLoggableContentType(String contentType) {
    if (contentType == null) {
      return false;
    }
    String lower = contentType.toLowerCase();
    return lower.contains("application/json")
        || lower.contains("application/x-www-form-urlencoded")
        || lower.contains("text/plain")
        || lower.contains("text/xml");
  }
}
