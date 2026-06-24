package com.sb10.mopl.common.log;

import jakarta.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** 로그 출력 시 민감한 정보(인증 토큰, 비밀번호, 개인정보, 금융정보 등)를 마스킹 처리하는 유틸리티 클래스입니다. */
public final class LogMaskingUtils {

  private static final Set<String> SENSITIVE_HEADERS =
      Set.of(
          "authorization",
          "proxy-authorization",
          "cookie",
          "set-cookie",
          "x-api-key",
          "x-api-token");

  private static final Set<String> SENSITIVE_KEYS =
      Set.of(
          "password",
          "passwordconfirm",
          "password_confirm",
          "accesstoken",
          "access_token",
          "refreshtoken",
          "refresh_token",
          "token",
          "apikey",
          "api_key",
          "secret",
          "secretkey",
          "secret_key",
          "client_secret",
          "cardnumber",
          "card_number",
          "cvc",
          "cvv",
          "accountnumber",
          "account_number",
          "ssn",
          "socialnumber",
          "social_number",
          "residentnumber",
          "resident_number",
          "phonenumber",
          "phone_number",
          "phone");

  // JSON Body용 정규식 패턴 (문자열 값 외에 숫자, 불리언, null 매칭 포함)
  private static final Pattern SENSITIVE_JSON_PATTERN =
      Pattern.compile(
          "\"(?i)(" + String.join("|", SENSITIVE_KEYS) + ")\"\\s*:\\s*(\"[^\"]*\"|[^,\\s}]+)");

  // Query Parameter 및 Form Body용 정규식 패턴
  private static final Pattern SENSITIVE_PARAM_PATTERN =
      Pattern.compile("(?i)(" + String.join("|", SENSITIVE_KEYS) + ")=([^&]+)");

  private LogMaskingUtils() {
    // 인스턴스화 방지
  }

  /**
   * HTTP 헤더 정보 중 민감한 값(Authorization 등)을 마스킹하여 반환합니다.
   *
   * @param request HTTP 요청 객체
   * @return 마스킹 처리된 헤더 정보 맵
   */
  public static Map<String, String> getMaskedHeaders(HttpServletRequest request) {
    return Collections.list(request.getHeaderNames()).stream()
        .collect(
            Collectors.toMap(
                name -> name,
                name -> maskHeaderValue(name, request.getHeader(name)),
                (existing, replacement) -> existing));
  }

  /**
   * 요청 파라미터 맵 중 민감한 파라미터 값을 마스킹하여 문자열로 반환합니다.
   *
   * @param request HTTP 요청 객체
   * @return 마스킹 처리된 파라미터 정보 문자열
   */
  public static String getMaskedRequestParams(HttpServletRequest request) {
    Map<String, String[]> parameterMap = request.getParameterMap();
    if (parameterMap == null || parameterMap.isEmpty()) {
      return "{}";
    }
    return parameterMap.entrySet().stream()
        .map(
            entry ->
                entry.getKey()
                    + "="
                    + (isSensitiveKey(entry.getKey())
                        ? "[******]"
                        : Arrays.toString(entry.getValue())))
        .collect(Collectors.joining(", ", "{", "}"));
  }

  /**
   * 쿼리 파라미터 문자열 내의 민감한 값을 마스킹하여 반환합니다.
   *
   * @param queryString 쿼리 파라미터 문자열
   * @return 마스킹 처리된 쿼리 문자열
   */
  public static String maskParameters(String queryString) {
    if (queryString == null || queryString.isEmpty()) {
      return "";
    }
    return SENSITIVE_PARAM_PATTERN.matcher(queryString).replaceAll("$1=******");
  }

  /**
   * 본문 바이트 배열 내의 JSON 및 Form 파라미터 민감한 키값들을 마스킹 처리하여 문자열로 반환합니다. 보안 조치로 마스킹 작업을 먼저 완벽히 수행한 뒤 길이 제한을
   * 적용합니다.
   *
   * @param content 본문 바이트 배열
   * @param encoding 문자 인코딩 방식
   * @return 마스킹 처리된 본문 문자열
   */
  public static String getMaskedBody(byte[] content, String encoding) {
    if (content == null || content.length == 0) {
      return "";
    }
    try {
      String charEncoding =
          encoding == null || "ISO-8859-1".equalsIgnoreCase(encoding) ? "UTF-8" : encoding;
      String body = new String(content, charEncoding);

      // 1) JSON 마스킹 적용 (전체 길이에 대해 수행)
      String maskedBody = SENSITIVE_JSON_PATTERN.matcher(body).replaceAll("\"$1\":\"******\"");
      // 2) Form parameter(form-urlencoded) 형태 마스킹 적용 (전체 길이에 대해 수행)
      String finalBody = SENSITIVE_PARAM_PATTERN.matcher(maskedBody).replaceAll("$1=******");

      // 3) 마스킹이 완료된 최종 로그 문자열에 대해 길이 자르기 적용 (민감정보 부분 유출 방지)
      if (finalBody.length() > 1024) {
        finalBody = finalBody.substring(0, 1024) + "... (truncated)";
      }
      return finalBody;
    } catch (UnsupportedEncodingException e) {
      return "[Unsupported Encoding]";
    }
  }

  /** HTTP 헤더 이름을 기준으로 특정 헤더의 민감 여부를 확인하여 마스킹합니다. */
  private static String maskHeaderValue(String name, String value) {
    if (name == null) {
      return value;
    }
    if (!SENSITIVE_HEADERS.contains(name.toLowerCase())) {
      return value;
    }
    if (value == null) {
      return "";
    }
    return name.equalsIgnoreCase("authorization") && value.toLowerCase().startsWith("bearer ")
        ? "Bearer ******"
        : "******";
  }

  /** 특정 키가 민감한 필드인지 여부를 확인합니다. */
  private static boolean isSensitiveKey(String key) {
    if (key == null) {
      return false;
    }
    String lowerKey = key.toLowerCase();
    return SENSITIVE_KEYS.stream().anyMatch(lowerKey::contains);
  }
}
