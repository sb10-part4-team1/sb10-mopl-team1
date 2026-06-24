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
  void createSuccessWhenNameIsValid() {
    // when
    Tag tag = Tag.create("스릴러");

    // then
    assertNotNull(tag);
    assertEquals("스릴러", tag.getName());
  }

  @Test
  @DisplayName("태그 이름이 비어 있거나 공백일 시 생성에 실패하고 예외를 발생시킨다")
  void createFailWhenNameIsBlank() {
    // when & then
    ContentException ex = assertThrows(ContentException.class, () -> Tag.create("   "));

    assertEquals(ContentErrorCode.INVALID_CONTENT_DATA, ex.getErrorCode());
    assertTrue(ex.getDetails().containsKey("name"));
  }

  @Test
  @DisplayName("태그 이름이 100자를 초과할 시 생성에 실패하고 예외를 발생시킨다")
  void createFailWhenNameTooLong() {
    // given
    String longName = "T".repeat(101);

    // when & then
    ContentException ex = assertThrows(ContentException.class, () -> Tag.create(longName));

    assertEquals(ContentErrorCode.INVALID_CONTENT_DATA, ex.getErrorCode());
    assertTrue(ex.getDetails().containsKey("name"));
  }
}
