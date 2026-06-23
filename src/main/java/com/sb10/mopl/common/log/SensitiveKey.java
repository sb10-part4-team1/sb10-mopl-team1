package com.sb10.mopl.common.log;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 로그 기록 시 마스킹 처리가 필요한 민감한 본문/파라미터 키 목록을 관리하는 Enum입니다. */
@Getter
@RequiredArgsConstructor
public enum SensitiveKey {
  PASSWORD("password"),
  PASSWORD_CONFIRM("passwordconfirm"),
  PASSWORD_CONFIRM_UNDERSCORE("password_confirm"),
  ACCESS_TOKEN("accesstoken"),
  ACCESS_TOKEN_UNDERSCORE("access_token"),
  REFRESH_TOKEN("refreshtoken"),
  REFRESH_TOKEN_UNDERSCORE("refresh_token"),
  TOKEN("token"),
  API_KEY("apikey"),
  API_KEY_UNDERSCORE("api_key"),
  SECRET("secret"),
  SECRET_KEY("secretkey"),
  SECRET_KEY_UNDERSCORE("secret_key"),
  CLIENT_SECRET("clientsecret"),
  CLIENT_SECRET_UNDERSCORE("client_secret"),
  CARD_NUMBER("cardnumber"),
  CARD_NUMBER_UNDERSCORE("card_number"),
  CVC("cvc"),
  CVV("cvv"),
  ACCOUNT_NUMBER("accountnumber"),
  ACCOUNT_NUMBER_UNDERSCORE("account_number"),
  SSN("ssn"),
  SOCIAL_NUMBER("socialnumber"),
  SOCIAL_NUMBER_UNDERSCORE("social_number"),
  RESIDENT_NUMBER("residentnumber"),
  RESIDENT_NUMBER_UNDERSCORE("resident_number"),
  PHONE_NUMBER("phonenumber"),
  PHONE_NUMBER_UNDERSCORE("phone_number"),
  PHONE("phone");

  private final String keyName;

  /**
   * 모든 민감한 키의 이름을 리스트로 반환합니다.
   *
   * @return 민감한 키 이름 리스트
   */
  public static List<String> getKeys() {
    return Arrays.stream(values()).map(SensitiveKey::getKeyName).collect(Collectors.toList());
  }
}
