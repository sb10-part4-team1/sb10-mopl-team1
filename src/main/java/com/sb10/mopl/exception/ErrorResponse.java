package com.sb10.mopl.exception;

import java.util.Map;

/**
 * 클라이언트에 반환할 표준화된 에러 응답 객체입니다.
 *
 * @param code 도메인별로 지정된 고유 에러 코드 번호 (예: "CT01")
 * @param message 에러에 관한 대략적인 원인 메시지 (예: "콘텐츠를 찾을 수 없습니다.")
 * @param details 디버깅 및 사용자 안내를 돕기 위한 상세 컨텍스트 데이터
 */
public record ErrorResponse(String code, String message, Map<String, Object> details) {}
