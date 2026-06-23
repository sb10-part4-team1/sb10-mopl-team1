package com.sb10.mopl.filter;

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
 * HTTP 요청 시마다 고유 Trace ID와 클라이언트 IP를 추출하여 MDC에 등록하고 관리하는 필터입니다. 서블릿 컨테이너의 최선두(Highest Precedence)에서
 * 실행됩니다.
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
   * @throws ServletException 서블릿 예외
   * @throws IOException 입출력 예외
   */
  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String traceId = UUID.randomUUID().toString().substring(0, 8);
    String clientIp = getClientIp(request);

    MDC.put(TRACE_ID_MDC_KEY, traceId);
    MDC.put(CLIENT_IP_MDC_KEY, clientIp);

    try {
      filterChain.doFilter(request, response);
    } finally {
      MDC.clear();
    }
  }

  /**
   * 요청 객체에서 클라이언트의 실제 IP 주소를 추출합니다.
   *
   * @param request HTTP 요청 객체
   * @return 클라이언트 IP 주소 문자열
   */
  private String getClientIp(HttpServletRequest request) {
    String ip = request.getHeader("X-Forwarded-For");
    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getHeader("Proxy-Client-IP");
    }
    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getHeader("WL-Proxy-Client-IP");
    }
    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getHeader("HTTP_CLIENT_IP");
    }
    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getHeader("HTTP_X_FORWARDED_FOR");
    }
    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getHeader("X-Real-IP");
    }
    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getRemoteAddr();
    }
    if (ip != null && ip.contains(",")) {
      ip = ip.split(",")[0].trim();
    }
    return ip;
  }
}
