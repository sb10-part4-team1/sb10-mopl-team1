package com.sb10.mopl.common.log;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;

/** HTTP 요청 객체로부터 클라이언트의 실제 IP 주소를 프록시 헤더 순으로 검사하여 추출하는 유틸리티 클래스입니다. */
public final class ClientIpExtractor {

  private static final List<String> IP_HEADERS =
      Arrays.asList(
          "X-Forwarded-For",
          "X-Real-IP",
          "Proxy-Client-IP",
          "WL-Proxy-Client-IP",
          "HTTP_CLIENT_IP",
          "HTTP_X_FORWARDED_FOR");

  private ClientIpExtractor() {
    // 인스턴스화 방지
  }

  /**
   * 프록시 헤더들을 검증하여 클라이언트의 실제 IP를 반환합니다.
   *
   * @param request HTTP 요청 객체
   * @return 클라이언트 IP 주소 문자열
   */
  public static String getClientIp(HttpServletRequest request) {
    String ip =
        IP_HEADERS.stream()
            .map(request::getHeader)
            .filter(
                header ->
                    header != null && !header.isEmpty() && !"unknown".equalsIgnoreCase(header))
            .findFirst()
            .orElse(request.getRemoteAddr());

    if (ip != null && ip.contains(",")) {
      ip = ip.split(",")[0].trim();
    }
    return ip;
  }
}
