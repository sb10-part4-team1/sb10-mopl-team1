package com.sb10.mopl.batch.interceptor;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

/*
 * 외부 API 통신 전용 HTTP 클라이언트 인터셉터입니다.
 *
 * 외부로 나가는 모든 HTTP 요청의 레이턴시(Timer)와 누적 성공/실패(Counter) 상태를 가로채어
 * 프로메테우스 메트릭으로 자동 적재합니다.
 */
@Component
@RequiredArgsConstructor
public class MetricsClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

  private final MeterRegistry meterRegistry;

  @Override
  public ClientHttpResponse intercept(
      HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

    // 1. 호스트 기반 클라이언트 종류(tmdb | sports) 추출
    String host = request.getURI().getHost();
    String client = "unknown";
    if (host != null) {
      if (host.contains("themoviedb")) {
        client = "tmdb";
      } else if (host.contains("thesportsdb")) {
        client = "sports";
      }
    }

    String path = request.getURI().getPath();
    long startTime = System.nanoTime();
    ClientHttpResponse response = null;
    String status = "error";

    try {
      response = execution.execute(request, body);
      status = String.valueOf(response.getStatusCode().value());
      return response;
    } catch (IOException e) {
      status = "io_error";
      throw e;
    } finally {
      long durationNano = System.nanoTime() - startTime;

      // 2. 카운터 기록: mopl.api.calls.total
      Counter.builder("mopl.api.calls.total")
          .description("Total number of external API calls")
          .tags("client", client, "path", path, "status", status)
          .register(meterRegistry)
          .increment();

      // 3. 타이머 기록: mopl.api.call.duration.seconds
      Timer.builder("mopl.api.call.duration.seconds")
          .description("Response latency of external API calls")
          .tags("client", client, "path", path)
          .register(meterRegistry)
          .record(durationNano, TimeUnit.NANOSECONDS);
    }
  }
}
