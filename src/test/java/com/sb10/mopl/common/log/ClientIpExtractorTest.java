package com.sb10.mopl.common.log;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ClientIpExtractorTest {

  @Nested
  @DisplayName("1. 클라이언트 IP 추출 성공 케이스")
  class SuccessCases {

    @Test
    @DisplayName("X-Forwarded-For 헤더에 쉼표로 연결된 IP 목록이 입력되면 첫 번째 IP를 반환한다")
    void getClientIp_returnsFirstIp_whenXforwardedForHasMultipleIps() {
      // given: X-Forwarded-For 헤더에 다중 IP가 포함된 목 요청 생성
      HttpServletRequest request = mock(HttpServletRequest.class);
      when(request.getHeader("X-Forwarded-For"))
          .thenReturn("203.0.113.195, 70.41.3.18, 150.172.238.178");

      // when: 클라이언트 IP 추출
      String clientIp = ClientIpExtractor.getClientIp(request);

      // that: 첫 번째 IP인 203.0.113.195가 추출되었는지 검증한다
      assertThat(clientIp).isEqualTo("203.0.113.195");
    }

    @Test
    @DisplayName("X-Real-IP 헤더만 유효하게 전달되면 해당 IP를 반환한다")
    void getClientIp_returnsXrealIp_whenXrealIpHeaderExists() {
      // given: X-Real-IP 헤더 설정
      HttpServletRequest request = mock(HttpServletRequest.class);
      when(request.getHeader("X-Real-IP")).thenReturn("198.51.100.1");

      // when: 클라이언트 IP 추출
      String clientIp = ClientIpExtractor.getClientIp(request);

      // that: 198.51.100.1 반환 검증
      assertThat(clientIp).isEqualTo("198.51.100.1");
    }

    @Test
    @DisplayName("프록시 헤더가 전혀 없으면 request.getRemoteAddr() 값을 반환한다")
    void getClientIp_returnsRemoteAddr_whenNoProxyHeadersExist() {
      // given: 헤더가 전혀 없고 getRemoteAddr만 있는 목 요청
      HttpServletRequest request = mock(HttpServletRequest.class);
      when(request.getRemoteAddr()).thenReturn("127.0.0.1");

      // when: 클라이언트 IP 추출
      String clientIp = ClientIpExtractor.getClientIp(request);

      // that: 127.0.0.1 반환 검증
      assertThat(clientIp).isEqualTo("127.0.0.1");
    }
  }

  @Nested
  @DisplayName("2. 클라이언트 IP 추출 엣지 케이스")
  class EdgeCases {

    @Test
    @DisplayName("request가 null이면 unknown 문자열을 반환한다")
    void getClientIp_returnsUnknown_whenRequestIsNull() {
      // when: null 요청 전달
      String clientIp = ClientIpExtractor.getClientIp(null);

      // that: "unknown" 검증한다
      assertThat(clientIp).isEqualTo("unknown");
    }

    @Test
    @DisplayName("프록시 헤더 값이 unknown이면 다음 우선순위 헤더 또는 RemoteAddr로 폴백된다")
    void getClientIp_fallbacks_whenHeaderIsUnknown() {
      // given: X-Forwarded-For는 unknown이고 X-Real-IP가 유효한 목 요청
      HttpServletRequest request = mock(HttpServletRequest.class);
      when(request.getHeader("X-Forwarded-For")).thenReturn("unknown");
      when(request.getHeader("X-Real-IP")).thenReturn("192.168.0.100");

      // when: 클라이언트 IP 추출
      String clientIp = ClientIpExtractor.getClientIp(request);

      // that: unknown을 스킵하고 192.168.0.100을 반환했는지 검증한다
      assertThat(clientIp).isEqualTo("192.168.0.100");
    }

    @Test
    @DisplayName("모든 헤더와 remoteAddr이 null이면 local 문자열을 반환한다")
    void getClientIp_returnsLocal_whenAllHeadersAndRemoteAddrAreNull() {
      // given: 모든 정보가 null인 목 요청
      HttpServletRequest request = mock(HttpServletRequest.class);
      when(request.getRemoteAddr()).thenReturn(null);

      // when: 클라이언트 IP 추출
      String clientIp = ClientIpExtractor.getClientIp(request);

      // that: "local" 검증한다
      assertThat(clientIp).isEqualTo("local");
    }
  }
}
