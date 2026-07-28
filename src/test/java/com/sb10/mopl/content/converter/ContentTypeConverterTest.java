package com.sb10.mopl.content.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sb10.mopl.content.entity.ContentType;
import com.sb10.mopl.content.exception.ContentErrorCode;
import com.sb10.mopl.content.exception.ContentException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ContentTypeConverterTest {

  private ContentTypeConverter converter;

  @BeforeEach
  void setUp() {
    converter = new ContentTypeConverter();
  }

  @Nested
  @DisplayName("1. ContentTypeConverter 정상 변환 및 null/공백 처리 검증")
  class SuccessCases {

    @Test
    @DisplayName("유효한 문자열(Name 또는 Value, 대소문자 구분 없음) 입력 시 올바른 ContentType으로 변환된다")
    void convert_success_whenValidStringProvided() {
      // given: 유효한 타입 문자열들을 준비한다
      String movieUpper = "MOVIE";
      String movieLower = "movie";
      String movieValue = ContentType.MOVIE.getValue();

      // when: 컨버터를 통해 변환을 수행할 때
      ContentType result1 = converter.convert(movieUpper);
      ContentType result2 = converter.convert(movieLower);
      ContentType result3 = converter.convert(movieValue);

      // that: 모두 ContentType.MOVIE로 올바르게 변환되는지 검증한다
      assertEquals(ContentType.MOVIE, result1);
      assertEquals(ContentType.MOVIE, result2);
      assertEquals(ContentType.MOVIE, result3);
    }

    @Test
    @DisplayName("null 또는 공백 문자열 입력 시 안전하게 null을 반환한다")
    void convert_returnsNull_whenNullOrBlankProvided() {
      // given: null 및 공백 문자열을 준비한다
      String nullSource = null;
      String blankSource = "   ";

      // when: 컨버터를 통해 변환을 수행할 때
      ContentType resultNull = converter.convert(nullSource);
      ContentType resultBlank = converter.convert(blankSource);

      // that: 두 결과 모두 null인지 검증한다
      assertNull(resultNull);
      assertNull(resultBlank);
    }
  }

  @Nested
  @DisplayName("2. ContentTypeConverter 부적절한 입력 예외 검증")
  class ExceptionCases {

    @Test
    @DisplayName("지원하지 않는 타입 문자열 입력 시 ContentException 예외를 발생시킨다")
    void convert_fail_whenInvalidStringProvided() {
      // given: 지원하지 않는 타입 문자열을 준비한다
      String invalidSource = "INVALID_TYPE_NAME";

      // when: 컨버터를 통해 변환을 수행할 때
      // that: INVALID_CONTENT_TYPE 에러 코드를 가진 예외가 발생하고 rejectedValue가 포함되는지 검증한다
      ContentException ex =
          assertThrows(ContentException.class, () -> converter.convert(invalidSource));

      assertEquals(ContentErrorCode.INVALID_CONTENT_TYPE, ex.getErrorCode());
      assertTrue(ex.getDetails().containsKey("rejectedValue"));
      assertEquals("INVALID_TYPE_NAME", ex.getDetails().get("rejectedValue"));
    }

    @Test
    @DisplayName("특수문자나 숫자가 포함된 무효한 입력 시 ContentException 예외를 발생시킨다")
    void convert_fail_whenSpecialCharactersProvided() {
      // given: 특수문자 및 숫자가 포함된 무효한 입력값을 준비한다
      String specialCharSource = "MOVIE_123!@#";

      // when: 컨버터를 통해 변환을 수행할 때
      // that: INVALID_CONTENT_TYPE 예외가 발생하고 rejectedValue가 포함되는지 검증한다
      ContentException ex =
          assertThrows(ContentException.class, () -> converter.convert(specialCharSource));

      assertEquals(ContentErrorCode.INVALID_CONTENT_TYPE, ex.getErrorCode());
      assertTrue(ex.getDetails().containsKey("rejectedValue"));
      assertEquals("MOVIE_123!@#", ex.getDetails().get("rejectedValue"));
    }
  }
}
