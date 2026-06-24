package com.sb10.mopl.common.validation;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/** 도메인 엔티티 내에서 유효성 검증을 수행하기 위한 유틸리티 클래스입니다. */
public final class DomainValidator {

  private final Map<String, Object> details = new HashMap<>();

  private DomainValidator() {}

  /**
   * 새로운 검증 체인을 시작합니다.
   *
   * @return DomainValidator 인스턴스
   */
  public static DomainValidator start() {
    return new DomainValidator();
  }

  /**
   * 조건을 검사하여 참인 경우(에러 상황) 세부 필드 에러 메시지를 추가합니다.
   *
   * @param invalidCondition 에러 조건 (참일 때 에러로 판단)
   * @param fieldName 검증 실패한 필드 이름
   * @param errorMessage 클라이언트에 반환할 에러 메시지
   * @return DomainValidator 인스턴스
   */
  public DomainValidator check(boolean invalidCondition, String fieldName, String errorMessage) {
    if (invalidCondition) {
      details.put(fieldName, errorMessage);
    }
    return this;
  }

  /**
   * 수집된 검증 실패 정보가 존재할 경우 예외를 생성하여 던집니다.
   *
   * @param exceptionFactory 에러 맵을 인자로 받아 구체적인 RuntimeException을 반환하는 함수형 인터페이스
   * @throws RuntimeException 검증 실패 시 생성된 예외
   */
  public void orThrow(Function<Map<String, Object>, ? extends RuntimeException> exceptionFactory) {
    if (!details.isEmpty()) {
      throw exceptionFactory.apply(details);
    }
  }
}
