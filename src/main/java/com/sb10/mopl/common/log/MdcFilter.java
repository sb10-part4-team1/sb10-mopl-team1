package com.sb10.mopl.common.log;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 모든 HTTP 요청에 대해 유니크한 traceId와 clientIp를 추출하여 Slf4j MDC에 바인딩하는 필터입니다. 필터 체인 중에서 가장 최우선 순위로 작동하여 이후
 * 모든 서블릿 로깅 및 보안 로깅에 traceId가 기록되도록 설계되었습니다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcFilter extends OncePerRequestFilter {

  private static final String TRACE_ID_MDC_KEY = "traceId";
  private static final String CLIENT_IP_MDC_KEY = "clientIp";

  /**
   * HTTP 요청을 가로채어 MDC 필드를 설정하고 필터 체인 처리 후 MDC를 초기화합니다.
   *
   * @param request HTTP 요청 객체
   * @param response HTTP 응답 객체
   * @param filterChain 필터 체인 객체
   */
  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String traceId = UUID.randomUUID().toString().substring(0, 8);
    String clientIp = ClientIpExtractor.getClientIp(request);

    MDC.put(TRACE_ID_MDC_KEY, traceId);
    MDC.put(CLIENT_IP_MDC_KEY, clientIp);

    try {
      filterChain.doFilter(request, response);
    } finally {
      MDC.clear();
    }
  }
}
