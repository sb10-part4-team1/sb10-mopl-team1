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
  void create_success_whenDataIsValid() {
    // given 생성자에 필요한 정상적인 입력값들을 지정한다
    String title = "기생충";
    ContentType type = ContentType.MOVIE;
    String description = "봉준호 감독의 명작";
    String thumbnailUrl = "https://image.url";

    // when 콘텐츠를 생성할 때
    Content content = Content.create(title, type, description, thumbnailUrl);

    // that 생성된 콘텐츠의 정보가 대입값 및 디폴트값(0.0, 0, 0L)과 일치하는지 무결성 검증한다
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
  void createContentTag_success_whenMapped() {
    // given 정상적인 콘텐츠와 태그 객체를 생성한다
    Content content = Content.create("기생충", ContentType.MOVIE, "봉준호 감독의 명작", "https://image.url");
    Tag tag = Tag.create("스릴러");

    // when 콘텐츠-태그 연관 매핑을 생성할 때
    ContentTag contentTag = ContentTag.create(content, tag);

    // that 콘텐츠 객체의 연관관계 리스트에 생성된 매핑 정보가 자동으로 동기화되었는지 검증한다
    assertEquals(1, content.getContentTags().size());
    assertEquals(contentTag, content.getContentTags().get(0));
    assertEquals(tag, content.getContentTags().get(0).getTag());
  }

  @Test
  @DisplayName("제목이 비어 있거나 공백일 시 콘텐츠 생성에 실패하고 예외를 발생시킨다")
  void create_fail_whenTitleIsBlank() {
    // given 공백으로 이루어진 제목 값을 입력한다
    String title = "  ";

    // when 콘텐츠를 생성할 때
    // that 예외가 발생하고 에러 맵에 title 필드가 포함되는지 검증한다
    ContentException ex =
        assertThrows(
            ContentException.class, () -> Content.create(title, ContentType.MOVIE, "설명", "url"));

    assertEquals(ContentErrorCode.INVALID_CONTENT_DATA, ex.getErrorCode());
    assertTrue(ex.getDetails().containsKey("title"));
  }

  @Test
  @DisplayName("제목이 100자를 초과할 시 콘텐츠 생성에 실패하고 예외를 발생시킨다")
  void create_fail_whenTitleTooLong() {
    // given 100자를 초과하는 제목 값을 준비한다
    String longTitle = "a".repeat(101);

    // when 콘텐츠를 생성할 때
    // that 예외가 발생하고 에러 맵에 title 필드가 포함되는지 검증한다
    ContentException ex =
        assertThrows(
            ContentException.class,
            () -> Content.create(longTitle, ContentType.MOVIE, "설명", "url"));

    assertEquals(ContentErrorCode.INVALID_CONTENT_DATA, ex.getErrorCode());
    assertTrue(ex.getDetails().containsKey("title"));
  }

  @Test
  @DisplayName("콘텐츠 형식이 null일 시 콘텐츠 생성에 실패하고 예외를 발생시킨다")
  void create_fail_whenTypeIsNull() {
    // given null 타입의 콘텐츠 형식을 전달한다
    ContentType type = null;

    // when 콘텐츠를 생성할 때
    // that 예외가 발생하고 에러 맵에 type 필드가 포함되는지 검증한다
    ContentException ex =
        assertThrows(ContentException.class, () -> Content.create("기생충", type, "설명", "url"));

    assertEquals(ContentErrorCode.INVALID_CONTENT_DATA, ex.getErrorCode());
    assertTrue(ex.getDetails().containsKey("type"));
  }

  @Test
  @DisplayName("설명이 공백이거나 null일 시 콘텐츠 생성에 실패하고 예외를 발생시킨다")
  void create_fail_whenDescriptionIsBlank() {
    // given 공백, null인 설명 값을 지정한다
    String description1 = null;
    String description2 = "   ";

    // when 콘텐츠를 생성할 때
    // that 예외가 발생하고 에러 맵에 description 필드가 포함되는지 검증한다
    ContentException ex1 =
        assertThrows(
            ContentException.class,
            () -> Content.create("기생충", ContentType.MOVIE, description1, "url"));

    ContentException ex2 =
        assertThrows(
            ContentException.class,
            () -> Content.create("기생충", ContentType.MOVIE, description2, "url"));

    assertEquals(ContentErrorCode.INVALID_CONTENT_DATA, ex1.getErrorCode());
    assertEquals(ContentErrorCode.INVALID_CONTENT_DATA, ex2.getErrorCode());
    assertTrue(ex1.getDetails().containsKey("description"));
    assertTrue(ex2.getDetails().containsKey("description"));
  }
}
