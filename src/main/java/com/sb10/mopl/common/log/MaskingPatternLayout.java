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
          "(?:\"?(?:"
              + String.join("|", SENSITIVE_KEYS) // matcher.group(0)
              + ")\"?\\s*[=:]\\s*\"?([^\",\\s&}]+)\"?)|" // matcher.group(1)
              + "(?:Authorization\\s*[=:]\\s*Bearer\\s+([a-zA-Z0-9_\\-\\.]+))", // matcher.group(2)
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
   * 2. matcher.group(0)은 그물에 걸린 전체 문자열인 "password=admin1234!" (matchGroup)이 됨.
   * 3. matcher.group(1)은 1번째 소괄호가 캡처한 실제 알맹이인 "admin1234!" (targetValue)이 됨.
   * 4. matchGroup.replace("admin1234!", "******")을 실행하여 "password=******" 로 변환 후 조립.
   */
  public String maskMessage(String message) {
    Matcher matcher = SENSITIVE_PATTERN.matcher(message);
    StringBuilder sb = new StringBuilder();
    int lastEnd = 0;

    // 정규식 패턴에 걸려든 민감 정보 문자열 조각 단위로 루프
    while (matcher.find()) {
      // 1. 이전 매칭의 끝지점부터 현재 발견된 매칭의 시작점까지의 안전한 원본 텍스트
      // (예: "Request: Method=[POST],Body=[username=admin&")를 먼저 붙임
      sb.append(message, lastEnd, matcher.start());

      // 2. 그물망에 걸려든 전체 문자열 껍데기를 0번 그룹에서 꺼냄 (예: "password=admin1234!")
      String matchGroup = matcher.group(0);

      // 3. 1번 그룹(일반 키워드 공용)과 2번 그룹(Bearer 헤더 전용) 중 매칭된 민감 알맹이 값을 확정적으로 획득 (논리상 무조건 존재함)
      String targetValue = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);

      // 4. 전체 껍데기("password=admin1234!") 내부에서 알맹이("admin1234!")만 "******" 로 즉시 치환하여 조립 (결과:
      // "password=******")
      sb.append(matchGroup.replace(targetValue, "******"));

      // 5. 다음 매칭 탐색을 시작할 오프셋 위치를 현재 매칭이 끝난 지점으로 갱신
      lastEnd = matcher.end();
    }
    // 6. 마지막 매칭 지점 이후부터 문장 끝까지의 남은 문자열 꼬리 부분(예: "]")을 덧붙임
    sb.append(message, lastEnd, message.length());
    return sb.toString();
  }
}
