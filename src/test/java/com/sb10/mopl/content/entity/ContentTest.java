package com.sb10.mopl.content.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sb10.mopl.content.exception.ContentErrorCode;
import com.sb10.mopl.content.exception.ContentException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ContentTest {

  @Nested
  @DisplayName("1. Content 엔티티 생성 및 비즈니스 기능 정상 동작 검증")
  class SuccessCases {

    @Test
    @DisplayName("정상 데이터를 입력하여 콘텐츠를 수동 생성할 경우 초기 상태로 성공적으로 생성된다")
    void create_success_whenDataIsValid() {
      // given: 생성에 필요한 유효한 입력값을 준비한다
      String title = "기생충";
      ContentType type = ContentType.MOVIE;
      String description = "봉준호 감독의 명작";
      String thumbnailUrl = "https://image.url/parasite.jpg";

      // when: 수동 생성 메서드를 호출할 때
      Content content = Content.create(title, type, description, thumbnailUrl);

      // that: 생성된 콘텐츠 정보 및 디폴트 통계값(0.0, 0, 0L)이 무결하게 설정되었는지 검증한다
      assertNotNull(content);
      assertEquals("기생충", content.getTitle());
      assertEquals(ContentType.MOVIE, content.getType());
      assertEquals("봉준호 감독의 명작", content.getDescription());
      assertEquals("https://image.url/parasite.jpg", content.getThumbnailUrl());
      assertEquals(ContentProvider.MANUAL, content.getProvider());
      assertEquals(0.0, content.getAverageRating());
      assertEquals(0, content.getReviewCount());
      assertEquals(0L, content.getWatcherCount());
    }

    @Test
    @DisplayName("외부 프로바이더 정보와 함께 생성할 경우 지정된 프로바이더 정보가 올바르게 설정된다")
    void createWithProvider_success_whenDataIsValid() {
      // given: 프로바이더 정보를 포함한 유효한 입력값을 준비한다
      String title = "인터스텔라";
      ContentType type = ContentType.MOVIE;
      String description = "우주 탐험 이야기";
      String thumbnailUrl = "https://image.url/interstellar.jpg";
      ContentProvider provider = ContentProvider.TMDB;
      String providerId = "tmdb_12345";

      // when: 프로바이더 지정 생성 메서드를 호출할 때
      Content content =
          Content.createWithProvider(title, type, description, thumbnailUrl, provider, providerId);

      // that: 프로바이더 정보와 ID가 올바르게 저장되었는지 검증한다
      assertEquals(ContentProvider.TMDB, content.getProvider());
      assertEquals("tmdb_12345", content.getProviderId());
    }

    @Test
    @DisplayName("유효한 변경 정보를 입력할 경우 콘텐츠 기본 정보가 성공적으로 수정된다")
    void update_success_whenDataIsValid() {
      // given: 기존 콘텐츠 객체와 변경할 수치값을 준비한다
      Content content =
          Content.create("기생충", ContentType.MOVIE, "기존 설명", "https://image.url/old.jpg");
      String newTitle = "기생충: 확장판";
      String newDescription = "더 깊어진 봉준호 감독의 명작";
      String newThumbnailUrl = "https://image.url/new.jpg";

      // when: 정보 업데이트를 수행할 때
      content.update(newTitle, newDescription, newThumbnailUrl);

      // that: 콘텐츠의 제목, 설명, 썸네일 URL이 성공적으로 변경되었는지 검증한다
      assertEquals("기생충: 확장판", content.getTitle());
      assertEquals("더 깊어진 봉준호 감독의 명작", content.getDescription());
      assertEquals("https://image.url/new.jpg", content.getThumbnailUrl());
    }

    @Test
    @DisplayName("유효한 평점과 리뷰 수를 전달할 경우 통계 정보가 성공적으로 수정된다")
    void updateStatistics_success_whenDataIsValid() {
      // given: 기존 콘텐츠 객체를 준비한다
      Content content = Content.create("기생충", ContentType.MOVIE, "봉준호 감독의 명작", "https://image.url");

      // when: 평점 4.5 및 리뷰 수 100으로 통계 정보를 수정할 때
      content.updateStatistics(4.5, 100);

      // that: 평균 평점과 리뷰 수가 성공적으로 반영되었는지 검증한다
      assertEquals(4.5, content.getAverageRating());
      assertEquals(100, content.getReviewCount());
    }

    @Test
    @DisplayName("유효한 시청자 수를 전달할 경우 시청자 수가 성공적으로 수정된다")
    void updateWatcherCount_success_whenDataIsValid() {
      // given: 기존 콘텐츠 객체를 준비한다
      Content content = Content.create("기생충", ContentType.MOVIE, "봉준호 감독의 명작", "https://image.url");

      // when: 시청자 수 500L로 수정할 때
      content.updateWatcherCount(500L);

      // that: 시청자 수가 성공적으로 반영되었는지 검증한다
      assertEquals(500L, content.getWatcherCount());
    }

    @Test
    @DisplayName("콘텐츠-태그 연관관계 매핑 시 양방향 연관관계가 자동으로 동기화된다")
    void createContentTag_success_whenMapped() {
      // given: 콘텐츠와 태그 객체를 준비한다
      Content content = Content.create("기생충", ContentType.MOVIE, "봉준호 감독의 명작", "https://image.url");
      Tag tag = Tag.create("스릴러");

      // when: ContentTag 매핑을 생성할 때
      ContentTag contentTag = ContentTag.create(content, tag);

      // that: 콘텐츠의 연관관계 리스트에 생성된 ContentTag가 정상 동기화되었는지 검증한다
      assertEquals(1, content.getContentTags().size());
      assertEquals(contentTag, content.getContentTags().get(0));
      assertEquals(tag, content.getContentTags().get(0).getTag());
    }
  }

  @Nested
  @DisplayName("2. Content 엔티티 생성 시 입력값 무결성 예외 검증")
  class CreateValidationCases {

    @Test
    @DisplayName("제목이 null이거나 공백일 시 생성에 실패하고 예외를 발생시킨다")
    void create_fail_whenTitleIsBlank() {
      // given: null 및 공백 제목 값을 준비한다
      String nullTitle = null;
      String blankTitle = "   ";

      // when: 콘텐츠를 생성하려고 할 때
      // that: INVALID_CONTENT_DATA 예외가 발생하고 상세 맵에 title 키가 존재하는지 검증한다
      ContentException ex1 =
          assertThrows(
              ContentException.class,
              () -> Content.create(nullTitle, ContentType.MOVIE, "설명", "https://image.url"));
      ContentException ex2 =
          assertThrows(
              ContentException.class,
              () -> Content.create(blankTitle, ContentType.MOVIE, "설명", "https://image.url"));

      assertEquals(ContentErrorCode.INVALID_CONTENT_DATA, ex1.getErrorCode());
      assertTrue(ex1.getDetails().containsKey("title"));
      assertEquals(ContentErrorCode.INVALID_CONTENT_DATA, ex2.getErrorCode());
      assertTrue(ex2.getDetails().containsKey("title"));
    }

    @Test
    @DisplayName("제목이 100자를 초과할 시 생성에 실패하고 예외를 발생시킨다")
    void create_fail_whenTitleTooLong() {
      // given: 101자의 초과 길이 제목을 준비한다
      String longTitle = "a".repeat(101);

      // when: 콘텐츠를 생성하려고 할 때
      // that: INVALID_CONTENT_DATA 예외가 발생하고 상세 맵에 title 키가 존재하는지 검증한다
      ContentException ex =
          assertThrows(
              ContentException.class,
              () -> Content.create(longTitle, ContentType.MOVIE, "설명", "https://image.url"));

      assertEquals(ContentErrorCode.INVALID_CONTENT_DATA, ex.getErrorCode());
      assertTrue(ex.getDetails().containsKey("title"));
    }

    @Test
    @DisplayName("콘텐츠 형식이 null일 시 생성에 실패하고 예외를 발생시킨다")
    void create_fail_whenTypeIsNull() {
      // given: null인 ContentType을 준비한다
      ContentType nullType = null;

      // when: 콘텐츠를 생성하려고 할 때
      // that: INVALID_CONTENT_DATA 예외가 발생하고 상세 맵에 type 키가 존재하는지 검증한다
      ContentException ex =
          assertThrows(
              ContentException.class,
              () -> Content.create("제목", nullType, "설명", "https://image.url"));

      assertEquals(ContentErrorCode.INVALID_CONTENT_DATA, ex.getErrorCode());
      assertTrue(ex.getDetails().containsKey("type"));
    }

    @Test
    @DisplayName("설명이 null이거나 공백일 시 생성에 실패하고 예외를 발생시킨다")
    void create_fail_whenDescriptionIsBlank() {
      // given: null 및 공백 설명 값을 준비한다
      String nullDesc = null;
      String blankDesc = "   ";

      // when: 콘텐츠를 생성하려고 할 때
      // that: INVALID_CONTENT_DATA 예외가 발생하고 상세 맵에 description 키가 존재하는지 검증한다
      ContentException ex1 =
          assertThrows(
              ContentException.class,
              () -> Content.create("제목", ContentType.MOVIE, nullDesc, "https://image.url"));
      ContentException ex2 =
          assertThrows(
              ContentException.class,
              () -> Content.create("제목", ContentType.MOVIE, blankDesc, "https://image.url"));

      assertEquals(ContentErrorCode.INVALID_CONTENT_DATA, ex1.getErrorCode());
      assertTrue(ex1.getDetails().containsKey("description"));
      assertEquals(ContentErrorCode.INVALID_CONTENT_DATA, ex2.getErrorCode());
      assertTrue(ex2.getDetails().containsKey("description"));
    }

    @Test
    @DisplayName("썸네일 URL이 null이거나 공백일 시 생성에 실패하고 예외를 발생시킨다")
    void create_fail_whenThumbnailUrlIsBlank() {
      // given: null 및 공백 썸네일 URL을 준비한다
      String nullUrl = null;
      String blankUrl = "   ";

      // when: 콘텐츠를 생성하려고 할 때
      // that: INVALID_CONTENT_DATA 예외가 발생하고 상세 맵에 thumbnailUrl 키가 존재하는지 검증한다
      ContentException ex1 =
          assertThrows(
              ContentException.class, () -> Content.create("제목", ContentType.MOVIE, "설명", nullUrl));
      ContentException ex2 =
          assertThrows(
              ContentException.class,
              () -> Content.create("제목", ContentType.MOVIE, "설명", blankUrl));

      assertEquals(ContentErrorCode.INVALID_CONTENT_DATA, ex1.getErrorCode());
      assertTrue(ex1.getDetails().containsKey("thumbnailUrl"));
      assertEquals(ContentErrorCode.INVALID_CONTENT_DATA, ex2.getErrorCode());
      assertTrue(ex2.getDetails().containsKey("thumbnailUrl"));
    }

    @Test
    @DisplayName("제공처(Provider)가 null일 시 프로바이더 기반 생성에 실패하고 예외를 발생시킨다")
    void createWithProvider_fail_whenProviderIsNull() {
      // given: null인 ContentProvider를 준비한다
      ContentProvider nullProvider = null;

      // when: 프로바이더 생성 메서드를 호출할 때
      // that: INVALID_CONTENT_DATA 예외가 발생하고 상세 맵에 provider 키가 존재하는지 검증한다
      ContentException ex =
          assertThrows(
              ContentException.class,
              () ->
                  Content.createWithProvider(
                      "제목", ContentType.MOVIE, "설명", "https://image.url", nullProvider, "id"));

      assertEquals(ContentErrorCode.INVALID_CONTENT_DATA, ex.getErrorCode());
      assertTrue(ex.getDetails().containsKey("provider"));
    }
  }

  @Nested
  @DisplayName("3. Content 엔티티 수정 시 입력값 무결성 예외 검증")
  class UpdateValidationCases {

    @Test
    @DisplayName("수정 시 제목이 공백이거나 100자 초과일 경우 예외를 발생시킨다")
    void update_fail_whenTitleIsInvalid() {
      // given: 기존 콘텐츠와 유효하지 않은 제목들을 준비한다
      Content content = Content.create("기생충", ContentType.MOVIE, "설명", "https://image.url");
      String blankTitle = "   ";
      String longTitle = "a".repeat(101);

      // when: 유효하지 않은 제목으로 수정하려고 할 때
      // that: 각각 예외가 발생하고 상세 맵에 title 키가 포함되는지 검증한다
      ContentException ex1 =
          assertThrows(
              ContentException.class, () -> content.update(blankTitle, "설명", "https://image.url"));
      ContentException ex2 =
          assertThrows(
              ContentException.class, () -> content.update(longTitle, "설명", "https://image.url"));

      assertEquals(ContentErrorCode.INVALID_CONTENT_DATA, ex1.getErrorCode());
      assertTrue(ex1.getDetails().containsKey("title"));
      assertEquals(ContentErrorCode.INVALID_CONTENT_DATA, ex2.getErrorCode());
      assertTrue(ex2.getDetails().containsKey("title"));
    }

    @Test
    @DisplayName("수정 시 설명이 공백이거나 null일 경우 예외를 발생시킨다")
    void update_fail_whenDescriptionIsBlank() {
      // given: 기존 콘텐츠와 유효하지 않은 설명들을 준비한다
      Content content = Content.create("기생충", ContentType.MOVIE, "설명", "https://image.url");
      String blankDesc = "   ";

      // when: 공백 설명으로 수정하려고 할 때
      // that: 예외가 발생하고 상세 맵에 description 키가 포함되는지 검증한다
      ContentException ex =
          assertThrows(
              ContentException.class, () -> content.update("제목", blankDesc, "https://image.url"));

      assertEquals(ContentErrorCode.INVALID_CONTENT_DATA, ex.getErrorCode());
      assertTrue(ex.getDetails().containsKey("description"));
    }

    @Test
    @DisplayName("수정 시 썸네일 URL이 공백이거나 null일 경우 예외를 발생시킨다")
    void update_fail_whenThumbnailUrlIsBlank() {
      // given: 기존 콘텐츠와 공백 URL을 준비한다
      Content content = Content.create("기생충", ContentType.MOVIE, "설명", "https://image.url");
      String blankUrl = "   ";

      // when: 공백 URL로 수정하려고 할 때
      // that: 예외가 발생하고 상세 맵에 thumbnailUrl 키가 포함되는지 검증한다
      ContentException ex =
          assertThrows(ContentException.class, () -> content.update("제목", "설명", blankUrl));

      assertEquals(ContentErrorCode.INVALID_CONTENT_DATA, ex.getErrorCode());
      assertTrue(ex.getDetails().containsKey("thumbnailUrl"));
    }

    @Test
    @DisplayName("평점이 0.0 미만이거나 5.0 초과일 경우 통계 수정에 실패하고 예외를 발생시킨다")
    void updateStatistics_fail_whenAverageRatingOutOfRange() {
      // given: 기존 콘텐츠를 준비한다
      Content content = Content.create("기생충", ContentType.MOVIE, "설명", "https://image.url");

      // when: 평점을 -0.1 또는 5.1로 수정하려고 할 때
      // that: 각각 예외가 발생하고 상세 맵에 averageRating 키가 포함되는지 검증한다
      ContentException ex1 =
          assertThrows(ContentException.class, () -> content.updateStatistics(-0.1, 10));
      ContentException ex2 =
          assertThrows(ContentException.class, () -> content.updateStatistics(5.1, 10));

      assertEquals(ContentErrorCode.INVALID_CONTENT_DATA, ex1.getErrorCode());
      assertTrue(ex1.getDetails().containsKey("averageRating"));
      assertEquals(ContentErrorCode.INVALID_CONTENT_DATA, ex2.getErrorCode());
      assertTrue(ex2.getDetails().containsKey("averageRating"));
    }

    @Test
    @DisplayName("리뷰 수가 음수일 경우 통계 수정에 실패하고 예외를 발생시킨다")
    void updateStatistics_fail_whenReviewCountNegative() {
      // given: 기존 콘텐츠를 준비한다
      Content content = Content.create("기생충", ContentType.MOVIE, "설명", "https://image.url");

      // when: 리뷰 수를 -1로 수정하려고 할 때
      // that: 예외가 발생하고 상세 맵에 reviewCount 키가 포함되는지 검증한다
      ContentException ex =
          assertThrows(ContentException.class, () -> content.updateStatistics(4.0, -1));

      assertEquals(ContentErrorCode.INVALID_CONTENT_DATA, ex.getErrorCode());
      assertTrue(ex.getDetails().containsKey("reviewCount"));
    }

    @Test
    @DisplayName("시청자 수가 음수일 경우 시청자 수 수정에 실패하고 예외를 발생시킨다")
    void updateWatcherCount_fail_whenWatcherCountNegative() {
      // given: 기존 콘텐츠를 준비한다
      Content content = Content.create("기생충", ContentType.MOVIE, "설명", "https://image.url");

      // when: 시청자 수를 -1L로 수정하려고 할 때
      // that: 예외가 발생하고 상세 맵에 watcherCount 키가 포함되는지 검증한다
      ContentException ex =
          assertThrows(ContentException.class, () -> content.updateWatcherCount(-1L));

      assertEquals(ContentErrorCode.INVALID_CONTENT_DATA, ex.getErrorCode());
      assertTrue(ex.getDetails().containsKey("watcherCount"));
    }
  }
}
