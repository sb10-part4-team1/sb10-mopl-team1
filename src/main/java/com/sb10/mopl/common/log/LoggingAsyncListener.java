package com.sb10.mopl.common.log;

import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import java.io.IOException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

/** Servlet 비동기 요청 완료 시점에 MDC를 복구하고 요청/응답 정보 로깅 및 바디 복사를 수행하는 리스너입니다. */
@RequiredArgsConstructor
class LoggingAsyncListener implements AsyncListener {

  private final ContentCachingRequestWrapper requestWrapper;
  private final ContentCachingResponseWrapper responseWrapper;
  private final long startTime;
  private final Map<String, String> mdcContext;
  private final RequestLoggingFilter filter;

  @Override
  public void onComplete(AsyncEvent event) throws IOException {
    if (mdcContext != null) {
      MDC.setContextMap(mdcContext);
    } else {
      MDC.clear();
    }
    try {
      filter.logAndCopyResponse(requestWrapper, responseWrapper, startTime);
    } finally {
      MDC.clear();
    }
  }

  @Override
  public void onTimeout(AsyncEvent event) throws IOException {}

  @Override
  public void onError(AsyncEvent event) throws IOException {}

  @Override
  public void onStartAsync(AsyncEvent event) throws IOException {}
}
