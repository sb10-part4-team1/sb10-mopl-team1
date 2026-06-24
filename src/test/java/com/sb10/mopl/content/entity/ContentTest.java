package com.sb10.mopl.content.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sb10.mopl.content.exception.ContentErrorCode;
import com.sb10.mopl.content.exception.ContentException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ContentTest {

  @Test
  @DisplayName("정상 데이터를 입력할 시 콘텐츠 객체가 성공적으로 생성된다")
  void createSuccessWhenDataIsValid() {
    // when
    Content content = Content.create("기생충", ContentType.MOVIE, "봉준호 감독의 명작", "https://image.url");

    // then
    assertNotNull(content);
    assertEquals("기생충", content.getTitle());
    assertEquals(ContentType.MOVIE, content.getType());
    assertEquals("봉준호 감독의 명작", content.getDescription());
    assertEquals("https://image.url", content.getThumbnailUrl());
    assertEquals(0.0, content.getAverageRating());
    assertEquals(0, content.getReviewCount());
    assertEquals(0L, content.getWatcherCount());
  }

  @Test
  @DisplayName("콘텐츠-태그 매핑 생성 시 양방향 연관관계 편의 로직에 의해 콘텐츠 객체에 매핑 정보가 자동으로 추가된다")
  void createContentTagConvenienceSuccess() {
    // given
    Content content = Content.create("기생충", ContentType.MOVIE, "봉준호 감독의 명작", "https://image.url");
    Tag tag = Tag.create("스릴러");

    // when
    ContentTag contentTag = ContentTag.create(content, tag);

    // then
    assertEquals(1, content.getContentTags().size());
    assertEquals(contentTag, content.getContentTags().get(0));
    assertEquals(tag, content.getContentTags().get(0).getTag());
  }

  @Test
  @DisplayName("제목이 비어 있거나 공백일 시 콘텐츠 생성에 실패하고 예외를 발생시킨다")
  void createFailWhenTitleIsBlank() {
    // when & then
    ContentException ex =
        assertThrows(
            ContentException.class, () -> Content.create("  ", ContentType.MOVIE, "설명", "url"));

    assertEquals(ContentErrorCode.INVALID_CONTENT_DATA, ex.getErrorCode());
    assertTrue(ex.getDetails().containsKey("title"));
  }

  @Test
  @DisplayName("제목이 100자를 초과할 시 콘텐츠 생성에 실패하고 예외를 발생시킨다")
  void createFailWhenTitleTooLong() {
    // given
    String longTitle = "a".repeat(101);

    // when & then
    ContentException ex =
        assertThrows(
            ContentException.class,
            () -> Content.create(longTitle, ContentType.MOVIE, "설명", "url"));

    assertEquals(ContentErrorCode.INVALID_CONTENT_DATA, ex.getErrorCode());
    assertTrue(ex.getDetails().containsKey("title"));
  }

  @Test
  @DisplayName("콘텐츠 형식이 null일 시 콘텐츠 생성에 실패하고 예외를 발생시킨다")
  void createFailWhenTypeIsNull() {
    // when & then
    ContentException ex =
        assertThrows(ContentException.class, () -> Content.create("기생충", null, "설명", "url"));

    assertEquals(ContentErrorCode.INVALID_CONTENT_DATA, ex.getErrorCode());
    assertTrue(ex.getDetails().containsKey("type"));
  }

  @Test
  @DisplayName("설명이 비어 있거나 null일 시 콘텐츠 생성에 실패하고 예외를 발생시킨다")
  void createFailWhenDescriptionIsBlank() {
    // when & then
    ContentException ex =
        assertThrows(
            ContentException.class, () -> Content.create("기생충", ContentType.MOVIE, null, "url"));

    assertEquals(ContentErrorCode.INVALID_CONTENT_DATA, ex.getErrorCode());
    assertTrue(ex.getDetails().containsKey("description"));
  }
}
