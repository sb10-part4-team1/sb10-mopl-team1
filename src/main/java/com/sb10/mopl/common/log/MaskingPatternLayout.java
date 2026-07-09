package com.sb10.mopl.common.log;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MaskingPatternLayout extends PatternLayout {

  // 마스킹 검열 대상이 되는 JSON/Query 파라미터 키 목록
  private static final List<String> SENSITIVE_KEYS =
      java.util.List.of("password", "accessToken", "refreshToken", "token");

  // 키 목록과 Authorization Bearer 토큰을 잡아내기 위한 정규식 패턴
  private static final Pattern SENSITIVE_PATTERN =
      Pattern.compile(
          "(?:\"?\\b(?:"
              + String.join("|", SENSITIVE_KEYS) // matcher.group(0)
              + ")\\b\"?\\s*[=:]\\s*\"?([^\",&}]+)\"?)|" // matcher.group(1)
              + "(?:\\bAuthorization\\b\\s*[=:]"
              + "\\s*Bearer\\s+([a-zA-Z0-9_\\-\\.]+))", // matcher.group(2)
          Pattern.CASE_INSENSITIVE);

  @Override
  public String doLayout(ILoggingEvent event) {
    // 1. 부모 레이아웃(PatternLayout)이 설정된 패턴(%d %logger 등)에 맞춰 포맷팅한 최종 문자열을 가져옴
    String message = super.doLayout(event);
    if (message == null || message.isEmpty()) {
      return message;
    }
    // 2. 가공된 최종 문자열 내부의 민감 정보를 마스킹하여 반환
    return maskMessage(message);
  }

  /*
   * [정규식 마스킹 처리 시뮬레이션]
   *
   * 입력 로그 예시:
   * "Request: Method=[POST], Body=[username=admin&password=admin1234!]"
   *
   * 1. matcher.find()가 로그를 스캔하다가 "password=admin1234!" 부분을 포착하여 루프 진입.
   * 2. valueStart와 valueEnd에 1번 소괄호(알맹이) 오프셋인 "admin1234!"의 시작/끝 지점이 잡힘.
   * 3. lastEnd(0)부터 valueStart("password="의 끝)까지 안전한 원본 텍스트를 먼저 덧붙임.
   * 4. 민감 정보 알맹이가 있던 자리에 "******"를 바로 붙여서 마스킹 적용.
   * 5. 다음 매칭을 위해 lastEnd를 valueEnd("admin1234!"의 끝)로 갱신하여 닫는 괄호나 쉼표 등 원본 유지.
   */
  public String maskMessage(String message) {
    Matcher matcher = SENSITIVE_PATTERN.matcher(message);
    StringBuilder sb = new StringBuilder();
    int lastEnd = 0;

    // 그물망 패턴에 걸려든 민감 정보 문자열 조각 단위로 징검다리를 건너듯이 루프
    while (matcher.find()) {
      // 1. 1번 통(일반 키워드)과 2번 통(Bearer 헤더) 중 실제 낚여 올라온 알맹이의 물리적 위치(오프셋)를 확정적으로 획득
      int valueStart = matcher.group(1) != null ? matcher.start(1) : matcher.start(2);
      int valueEnd = matcher.group(1) != null ? matcher.end(1) : matcher.end(2);

      // 2. 이전 매칭의 끝지점부터 현재 낚인 민감한 알맹이(값)의 시작 오프셋까지 안전한 원본 문자열을 복사 (이로 인해 키와 구분자가 온전히 보존됨)
      sb.append(message, lastEnd, valueStart);

      // 3. 민감 정보 알맹이가 들어가야 할 위치에 "******" 로 정밀 치환 적용
      sb.append("******");

      // 4. 다음 매칭 탐색을 위해 시작 오프셋을 방금 조작한 민감 값의 끝 지점으로 갱신
      lastEnd = valueEnd;
    }
    // 5. 마지막 매칭 지점 이후부터 문장 끝까지의 남은 문자열 꼬리 부분을 덧붙임
    sb.append(message, lastEnd, message.length());
    return sb.toString();
  }
}
