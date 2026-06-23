package com.sb10.mopl.filter;

import jakarta.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** 로그 출력 시 민감한 정보(인증 토큰, 비밀번호, 개인정보, 금융정보 등)를 마스킹 처리하는 유틸리티 클래스입니다. */
public final class LogMaskingUtils {

  private static final List<String> SENSITIVE_HEADERS =
      Arrays.asList(
          "authorization",
          "proxy-authorization",
          "cookie",
          "set-cookie",
          "x-api-key",
          "x-api-token");

  private static final List<String> SENSITIVE_KEYS =
      Arrays.asList(
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
          "clientsecret",
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

  private static final Pattern SENSITIVE_JSON_PATTERN =
      Pattern.compile("\"(?i)(" + String.join("|", SENSITIVE_KEYS) + ")\"\\s*:\\s*\"([^\"]+)\"");

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
    if (parameterMap.isEmpty()) {
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
   * 본문 바이트 배열 내의 JSON 및 Form 파라미터 민감한 키값들을 마스킹 처리하여 문자열로 반환합니다.
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
      if (body.length() > 1024) {
        body = body.substring(0, 1024) + "... (truncated)";
      }
      String maskedBody = SENSITIVE_JSON_PATTERN.matcher(body).replaceAll("\"$1\":\"******\"");
      return SENSITIVE_PARAM_PATTERN.matcher(maskedBody).replaceAll("$1=******");
    } catch (UnsupportedEncodingException e) {
      return "[Unsupported Encoding]";
    }
  }

  /** HTTP 헤더 이름을 기준으로 특정 헤더의 민감 여부를 확인하여 마스킹합니다. */
  private static String maskHeaderValue(String name, String value) {
    if (!SENSITIVE_HEADERS.contains(name.toLowerCase())) {
      return value;
    }
    return name.equalsIgnoreCase("authorization") && value.toLowerCase().startsWith("bearer ")
        ? "Bearer ******"
        : "******";
  }

  /** 특정 키가 민감한 필드인지 여부를 확인합니다. */
  private static boolean isSensitiveKey(String key) {
    String lowerKey = key.toLowerCase();
    return SENSITIVE_KEYS.stream().anyMatch(lowerKey::contains);
  }
}
