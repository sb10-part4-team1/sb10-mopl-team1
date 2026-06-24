package com.sb10.mopl.content.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sb10.mopl.content.exception.ContentErrorCode;
import com.sb10.mopl.content.exception.ContentException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TagTest {

  @Test
  @DisplayName("정상적인 태그 이름을 지정하여 태그를 생성할 시 성공한다")
  void create_success_whenNameIsValid() {
    // given 태그 이름을 정상으로 준다
    String name = "스릴러";

    // when 태그를 생성할 때
    Tag tag = Tag.create(name);

    // that 생성된 객체가 null이 아니고 이름이 일치하는지 무결성 검증한다
    assertNotNull(tag);
    assertEquals("스릴러", tag.getName());
  }

  @Test
  @DisplayName("태그 이름이 비어 있거나 공백일 시 생성에 실패하고 예외를 발생시킨다")
  void create_fail_whenNameIsBlank() {
    // given 태그 이름에 공백을 준다
    String name = "   ";

    // when 태그를 생성할 때
    // that 예외가 발생하며 에러 맵에 name 필드가 포함되는지 검증한다
    ContentException ex = assertThrows(ContentException.class, () -> Tag.create(name));

    assertEquals(ContentErrorCode.INVALID_CONTENT_DATA, ex.getErrorCode());
    assertTrue(ex.getDetails().containsKey("name"));
  }

  @Test
  @DisplayName("태그 이름이 100자를 초과할 시 생성에 실패하고 예외를 발생시킨다")
  void create_fail_whenNameTooLong() {
    // given 100자를 초과하는 태그 이름을 준다
    String longName = "T".repeat(101);

    // when 태그를 생성할 때
    // that 예외가 발생하며 에러 맵에 name 필드가 포함되는지 검증한다
    ContentException ex = assertThrows(ContentException.class, () -> Tag.create(longName));

    assertEquals(ContentErrorCode.INVALID_CONTENT_DATA, ex.getErrorCode());
    assertTrue(ex.getDetails().containsKey("name"));
  }
}
