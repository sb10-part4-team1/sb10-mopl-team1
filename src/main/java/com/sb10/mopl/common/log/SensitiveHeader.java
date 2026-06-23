package com.sb10.mopl.common.log;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 로그 기록 시 마스킹 처리가 필요한 민감한 HTTP 헤더 목록을 관리하는 Enum입니다. */
@Getter
@RequiredArgsConstructor
public enum SensitiveHeader {
  AUTHORIZATION("authorization"),
  PROXY_AUTHORIZATION("proxy-authorization"),
  COOKIE("cookie"),
  SET_COOKIE("set-cookie"),
  X_API_KEY("x-api-key"),
  X_API_TOKEN("x-api-token");

  private final String headerName;

  /**
   * 모든 민감한 헤더의 소문자 이름을 리스트로 반환합니다.
   *
   * @return 소문자 헤더 이름 리스트
   */
  public static List<String> getNames() {
    return Arrays.stream(values()).map(SensitiveHeader::getHeaderName).collect(Collectors.toList());
  }
}
