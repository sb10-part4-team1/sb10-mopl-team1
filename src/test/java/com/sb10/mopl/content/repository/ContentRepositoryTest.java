package com.sb10.mopl.content.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sb10.mopl.common.pagination.SortDirection;
import com.sb10.mopl.config.JpaAuditingConfig;
import com.sb10.mopl.config.QuerydslConfig;
import com.sb10.mopl.content.dto.ContentSearchRequest;
import com.sb10.mopl.content.dto.ContentSortBy;
import com.sb10.mopl.content.entity.Content;
import com.sb10.mopl.content.entity.ContentTag;
import com.sb10.mopl.content.entity.ContentType;
import com.sb10.mopl.content.entity.Tag;
import com.sb10.mopl.content.exception.ContentErrorCode;
import com.sb10.mopl.content.exception.ContentException;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaAuditingConfig.class, QuerydslConfig.class})
class ContentRepositoryTest {

  @Autowired private ContentRepository contentRepository;
  @Autowired private TagRepository tagRepository;
  @Autowired private EntityManager em;

  @Nested
  @DisplayName("1. 기본 CRUD 및 영속화 검증")
  class SaveTests {

    @Test
    @DisplayName("올바른 콘텐츠 엔티티를 저장할 경우 UUID 식별자가 자동 생성되고 정상 영속화된다")
    void save_success_whenContentDataIsValid() {
      // given: 정상적인 콘텐츠 엔티티를 생성한다
      Content content =
          Content.create("인셉션", ContentType.MOVIE, "SF 스릴러 영화", "/uploads/inception.jpg");

      // when: 리포지토리에 저장할 때
      Content savedContent = contentRepository.save(content);

      // that: UUID ID가 할당되고 데이터가 일치하는지 검증한다
      assertThat(savedContent.getId()).isNotNull();
      assertThat(savedContent.getTitle()).isEqualTo("인셉션");
      assertThat(savedContent.getType()).isEqualTo(ContentType.MOVIE);
    }
  }

  @Nested
  @DisplayName("2. 조건 필터링 검증 (카테고리, 키워드, 태그)")
  class FilterTests {

    @Test
    @DisplayName("콘텐츠 타입(Type) 조건 지정 시 해당 타입의 콘텐츠만 정상 필터링된다")
    void findAllByCondition_success_whenTypeFilterProvided() {
      // given: MOVIE 타입과 TV_SERIES 타입 콘텐츠를 저장한다
      Content movie = Content.create("인셉션", ContentType.MOVIE, "영화 설명", "/url1");
      Content tv = Content.create("시그널", ContentType.TV_SERIES, "드라마 설명", "/url2");
      contentRepository.save(movie);
      contentRepository.save(tv);
      em.flush();
      em.clear();

      // when: ContentType.MOVIE 조건으로 조회할 때
      ContentSearchRequest request =
          new ContentSearchRequest(
              ContentType.MOVIE,
              null,
              null,
              null,
              null,
              10,
              SortDirection.DESCENDING,
              ContentSortBy.createdAt);
      List<Content> result = contentRepository.findAllByCondition(request);
      long count = contentRepository.countContents(request);

      // that: MOVIE 타입 콘텐츠만 1개 조회되고 count가 1인지 검증한다
      assertThat(result).hasSize(1);
      assertThat(result.get(0).getTitle()).isEqualTo("인셉션");
      assertThat(count).isEqualTo(1L);
    }

    @Test
    @DisplayName("키워드 검색 시 제목 또는 설명에 해당 키워드가 포함된 콘텐츠가 조회되며 앞뒤 공백이 다듬어진다")
    void findAllByCondition_success_whenKeywordMatchesTitleOrDescription() {
      // given: 제목에 'SF'가 포함된 콘텐츠와 설명에 'SF'가 포함된 콘텐츠를 저장한다
      Content content1 = Content.create("SF 대작 인터스텔라", ContentType.MOVIE, "우주 여행", "/url1");
      Content content2 = Content.create("기생충", ContentType.MOVIE, "SF 요소가 있는 영화", "/url2");
      Content content3 = Content.create("오징어 게임", ContentType.TV_SERIES, "데스 게임", "/url3");
      contentRepository.saveAll(List.of(content1, content2, content3));
      em.flush();
      em.clear();

      // when: 앞뒤 공백이 포함된 "  SF  " 키워드로 검색할 때
      ContentSearchRequest request =
          new ContentSearchRequest(
              null,
              "  SF  ",
              null,
              null,
              null,
              10,
              SortDirection.DESCENDING,
              ContentSortBy.createdAt);
      List<Content> result = contentRepository.findAllByCondition(request);

      // that: 제목 또는 설명에 SF가 포함된 2개의 콘텐츠가 조회되는지 검증한다
      assertThat(result).hasSize(2);
      assertThat(result)
          .extracting(Content::getTitle)
          .containsExactlyInAnyOrder("SF 대작 인터스텔라", "기생충");
    }

    @Test
    @DisplayName("태그 목록 검색 시 해당 태그 중 하나 이상을 보유한 콘텐츠가 중복 없이 조회된다")
    void findAllByCondition_success_whenTagMatches() {
      // given: 태그 및 콘텐츠-태그 매핑을 생성한다
      Tag action = tagRepository.save(Tag.create("액션"));
      Tag drama = tagRepository.save(Tag.create("드라마"));

      Content content1 = Content.create("액션 영화 1", ContentType.MOVIE, "설명", "/url1");
      ContentTag.create(content1, action);

      Content content2 = Content.create("드라마 1", ContentType.TV_SERIES, "설명", "/url2");
      ContentTag.create(content2, drama);

      Content content3 = Content.create("액션&드라마", ContentType.MOVIE, "설명", "/url3");
      ContentTag.create(content3, action);
      ContentTag.create(content3, drama);

      contentRepository.saveAll(List.of(content1, content2, content3));
      em.flush();
      em.clear();

      // when: "액션" 태그로 검색할 때
      ContentSearchRequest request =
          new ContentSearchRequest(
              null,
              null,
              List.of("액션"),
              null,
              null,
              10,
              SortDirection.DESCENDING,
              ContentSortBy.createdAt);
      List<Content> result = contentRepository.findAllByCondition(request);

      // that: 액션 태그를 보유한 2개의 콘텐츠가 올바르게 조회되는지 검증한다
      assertThat(result).hasSize(2);
      assertThat(result)
          .extracting(Content::getTitle)
          .containsExactlyInAnyOrder("액션 영화 1", "액션&드라마");
    }

    @Test
    @DisplayName("존재하지 않는 태그로 검색할 시 빈 결과를 반환한다")
    void findAllByCondition_returnsEmpty_whenNonExistentTagProvided() {
      // given: 기존 콘텐츠와 태그를 저장한다
      Content content = Content.create("인셉션", ContentType.MOVIE, "설명", "/url");
      contentRepository.save(content);
      em.flush();
      em.clear();

      // when: 존재하지 않는 태그 "공포"로 검색할 때
      ContentSearchRequest request =
          new ContentSearchRequest(
              null,
              null,
              List.of("공포"),
              null,
              null,
              10,
              SortDirection.DESCENDING,
              ContentSortBy.createdAt);
      List<Content> result = contentRepository.findAllByCondition(request);
      long count = contentRepository.countContents(request);

      // that: 결과 리스트가 비어있고 count가 0L인지 검증한다
      assertThat(result).isEmpty();
      assertThat(count).isEqualTo(0L);
    }
  }

  @Nested
  @DisplayName("3. 다양한 정렬(인기순, 평점순, 생성일순) 및 Tie-breaker 처리 검증")
  class SortTests {

    @Test
    @DisplayName("인기순(POPULAR) 정렬 시 시청자수 -> 리뷰수 -> ID 동률(Tie-breaker) 순으로 내림차순 정렬된다")
    void findAllByCondition_success_popularSortWithTieBreaker() {
      // given: 시청자수 및 리뷰수가 다른 콘텐츠들과 동일한 수치를 가진 콘텐츠들을 생성한다
      Content content1 = Content.create("인기 1위", ContentType.MOVIE, "설명", "/url1");
      content1.updateWatcherCount(1000L);
      content1.updateStatistics(4.0, 50);

      Content content2 = Content.create("인기 2위(리뷰수 우세)", ContentType.MOVIE, "설명", "/url2");
      content2.updateWatcherCount(500L);
      content2.updateStatistics(4.0, 100);

      Content content3 = Content.create("인기 3위(리뷰수 열세)", ContentType.MOVIE, "설명", "/url3");
      content3.updateWatcherCount(500L);
      content3.updateStatistics(4.0, 20);

      contentRepository.saveAll(List.of(content1, content2, content3));
      em.flush();
      em.clear();

      // when: watcherCount 내림차순 정렬로 조회할 때
      ContentSearchRequest request =
          new ContentSearchRequest(
              null,
              null,
              null,
              null,
              null,
              10,
              SortDirection.DESCENDING,
              ContentSortBy.watcherCount);
      List<Content> result = contentRepository.findAllByCondition(request);

      // that: 시청자수 1000 -> (시청자수 500, 리뷰 100) -> (시청자수 500, 리뷰 20) 순서로 정렬되는지 검증한다
      assertThat(result).hasSize(3);
      assertThat(result.get(0).getTitle()).isEqualTo("인기 1위");
      assertThat(result.get(1).getTitle()).isEqualTo("인기 2위(리뷰수 우세)");
      assertThat(result.get(2).getTitle()).isEqualTo("인기 3위(리뷰수 열세)");
    }

    @Test
    @DisplayName("평점순(RATING) 정렬 시 averageRating 내림차순으로 정렬된다")
    void findAllByCondition_success_ratingSortDescending() {
      // given: 평점이 다른 콘텐츠들을 구성한다
      Content contentLow = Content.create("낮은 평점", ContentType.MOVIE, "설명", "/url1");
      contentLow.updateStatistics(2.5, 10);

      Content contentHigh = Content.create("높은 평점", ContentType.MOVIE, "설명", "/url2");
      contentHigh.updateStatistics(4.9, 10);

      contentRepository.saveAll(List.of(contentLow, contentHigh));
      em.flush();
      em.clear();

      // when: 평점(rate) 내림차순 정렬로 조회할 때
      ContentSearchRequest request =
          new ContentSearchRequest(
              null, null, null, null, null, 10, SortDirection.DESCENDING, ContentSortBy.rate);
      List<Content> result = contentRepository.findAllByCondition(request);

      // that: 높은 평점 콘텐츠가 1순위로 조회되는지 검증한다
      assertThat(result).hasSize(2);
      assertThat(result.get(0).getTitle()).isEqualTo("높은 평점");
      assertThat(result.get(1).getTitle()).isEqualTo("낮은 평점");
    }
  }

  @Nested
  @DisplayName("4. 커서 기반 페이지네이션 검증")
  class CursorPagingTests {

    @Test
    @DisplayName("커서와 idAfter 정보를 전달할 시 다음 페이지 콘텐츠가 정확히 조회된다")
    void findAllByCondition_success_nextPagePaging() {
      // given: 생성 시각이 서로 다른 3개의 콘텐츠를 저장한다
      Instant now = Instant.now();
      Content c1 = Content.create("1번째 콘텐츠", ContentType.MOVIE, "설명", "/url1");
      org.springframework.test.util.ReflectionTestUtils.setField(
          c1, "createdAt", now.minusSeconds(20));

      Content c2 = Content.create("2번째 콘텐츠", ContentType.MOVIE, "설명", "/url2");
      org.springframework.test.util.ReflectionTestUtils.setField(
          c2, "createdAt", now.minusSeconds(10));

      Content c3 = Content.create("3번째 콘텐츠", ContentType.MOVIE, "설명", "/url3");
      org.springframework.test.util.ReflectionTestUtils.setField(c3, "createdAt", now);

      contentRepository.saveAll(List.of(c1, c2, c3));

      em.flush();
      em.clear();

      // when: limit = 2로 첫 페이지 조회 (생성일 오름차순)
      ContentSearchRequest firstPageReq =
          new ContentSearchRequest(
              null, null, null, null, null, 2, SortDirection.ASCENDING, ContentSortBy.createdAt);
      List<Content> firstPageResult = contentRepository.findAllByCondition(firstPageReq);

      // limit + 1개 조회되어 총 3개가 반환되고, 실제 슬라이스 2번째 항목은 c2이다
      assertThat(firstPageResult).hasSize(3);
      Content lastItem = firstPageResult.get(1); // c2

      // when: c2의 커서 정보(createdAt ISO문자열 및 idAfter)를 전달하여 두 번째 페이지 조회
      ContentSearchRequest secondPageReq =
          new ContentSearchRequest(
              null,
              null,
              null,
              lastItem.getCreatedAt().toString(),
              lastItem.getId(),
              2,
              SortDirection.ASCENDING,
              ContentSortBy.createdAt);
      List<Content> secondPageResult = contentRepository.findAllByCondition(secondPageReq);

      // that: 두 번째 페이지에서는 3번째 콘텐츠(c3)만 1개 조회되는지 검증한다
      assertThat(secondPageResult).hasSize(1);
      assertThat(secondPageResult.get(0).getTitle()).isEqualTo("3번째 콘텐츠");
    }

    @Test
    @DisplayName("인기순 정렬에서 커서(watcherCount_reviewCount) 전달 시 다음 순위 페이지가 정확히 조회된다")
    void findAllByCondition_success_nextPagePagingWithPopularSort() {
      // given: 인기 수치가 다른 3개의 콘텐츠를 저장한다
      Content c1 = Content.create("1위", ContentType.MOVIE, "설명", "/url1");
      c1.updateWatcherCount(1000L);
      c1.updateStatistics(4.5, 100);

      Content c2 = Content.create("2위", ContentType.MOVIE, "설명", "/url2");
      c2.updateWatcherCount(500L);
      c2.updateStatistics(4.0, 50);

      Content c3 = Content.create("3위", ContentType.MOVIE, "설명", "/url3");
      c3.updateWatcherCount(100L);
      c3.updateStatistics(3.0, 10);

      contentRepository.saveAll(List.of(c1, c2, c3));
      em.flush();
      em.clear();

      // when: c1 커서("1000_100", c1.getId()) 정보로 2위 이하 페이지 조회
      ContentSearchRequest request =
          new ContentSearchRequest(
              null,
              null,
              null,
              "1000_100",
              c1.getId(),
              10,
              SortDirection.DESCENDING,
              ContentSortBy.watcherCount);
      List<Content> result = contentRepository.findAllByCondition(request);

      // that: 1위 c1 제외하고 2위, 3위 콘텐츠만 순서대로 조회되는지 검증한다
      assertThat(result).hasSize(2);
      assertThat(result.get(0).getTitle()).isEqualTo("2위");
      assertThat(result.get(1).getTitle()).isEqualTo("3위");
    }
  }

  @Nested
  @DisplayName("5. 커서 비정상 입력 및 엣지 케이스 예외 검증")
  class CursorValidationEdgeCases {

    @Test
    @DisplayName(
        "cursor는 존재하지만 idAfter가 null인 불일치 요청 시 ContentException(INVALID_CURSOR_VALUE) 예외가 발생한다")
    void findAllByCondition_fail_whenCursorExistsButIdAfterIsNull() {
      // given: cursor 값만 있고 idAfter가 null인 파라미터를 구성한다
      ContentSearchRequest request =
          new ContentSearchRequest(
              null,
              null,
              null,
              "2026-07-27T00:00:00Z",
              null,
              10,
              SortDirection.DESCENDING,
              ContentSortBy.createdAt);

      // when & that: 조회 실행 시 INVALID_CURSOR_VALUE 예외가 발생하는지 검증한다
      assertThatThrownBy(() -> contentRepository.findAllByCondition(request))
          .isInstanceOf(ContentException.class)
          .hasFieldOrPropertyWithValue("errorCode", ContentErrorCode.INVALID_CURSOR_VALUE);
    }

    @Test
    @DisplayName("idAfter는 존재하고 cursor가 공백인 불일치 요청 시 INVALID_CURSOR_VALUE 예외가 발생한다")
    void findAllByCondition_fail_whenIdAfterExistsButCursorIsBlank() {
      // given: idAfter는 존재하나 cursor가 공백인 파라미터를 구성한다
      ContentSearchRequest request =
          new ContentSearchRequest(
              null,
              null,
              null,
              "   ",
              UUID.randomUUID(),
              10,
              SortDirection.DESCENDING,
              ContentSortBy.createdAt);

      // when & that: 조회 실행 시 INVALID_CURSOR_VALUE 예외가 발생하는지 검증한다
      assertThatThrownBy(() -> contentRepository.findAllByCondition(request))
          .isInstanceOf(ContentException.class)
          .hasFieldOrPropertyWithValue("errorCode", ContentErrorCode.INVALID_CURSOR_VALUE);
    }

    @Test
    @DisplayName("인기순 정렬에서 cursor 포맷이 시청자수_리뷰수 형태가 아닌 파싱 불가 문자열인 경우 ContentException 예외가 발생한다")
    void findAllByCondition_fail_whenPopularCursorFormatInvalid() {
      // given: 언더스코어가 없는 잘못된 커서 포맷("1000")을 전달한다
      ContentSearchRequest request =
          new ContentSearchRequest(
              null,
              null,
              null,
              "1000",
              UUID.randomUUID(),
              10,
              SortDirection.DESCENDING,
              ContentSortBy.watcherCount);

      // when & that: 조회 실행 시 INVALID_CURSOR_VALUE 예외가 발생하는지 검증한다
      assertThatThrownBy(() -> contentRepository.findAllByCondition(request))
          .isInstanceOf(ContentException.class)
          .hasFieldOrPropertyWithValue("errorCode", ContentErrorCode.INVALID_CURSOR_VALUE);
    }

    @Test
    @DisplayName("평점순 정렬에서 cursor 포맷이 숫자가 아닌 문자열인 경우 ContentException 예외가 발생한다")
    void findAllByCondition_fail_whenRateCursorFormatInvalid() {
      // given: 평점에 숫자가 아닌 "not_a_number" 커서를 전달한다
      ContentSearchRequest request =
          new ContentSearchRequest(
              null,
              null,
              null,
              "not_a_number",
              UUID.randomUUID(),
              10,
              SortDirection.DESCENDING,
              ContentSortBy.rate);

      // when & that: 조회 실행 시 INVALID_CURSOR_VALUE 예외가 발생하는지 검증한다
      assertThatThrownBy(() -> contentRepository.findAllByCondition(request))
          .isInstanceOf(ContentException.class)
          .hasFieldOrPropertyWithValue("errorCode", ContentErrorCode.INVALID_CURSOR_VALUE);
    }

    @Test
    @DisplayName("생성일순 정렬에서 cursor 포맷이 날짜 형태가 아닌 문자열인 경우 ContentException 예외가 발생한다")
    void findAllByCondition_fail_whenCreatedAtCursorFormatInvalid() {
      // given: 날짜 형태가 아닌 "invalid_date_format" 커서를 전달한다
      ContentSearchRequest request =
          new ContentSearchRequest(
              null,
              null,
              null,
              "invalid_date_format",
              UUID.randomUUID(),
              10,
              SortDirection.DESCENDING,
              ContentSortBy.createdAt);

      // when & that: 조회 실행 시 INVALID_CURSOR_VALUE 예외가 발생하는지 검증한다
      assertThatThrownBy(() -> contentRepository.findAllByCondition(request))
          .isInstanceOf(ContentException.class)
          .hasFieldOrPropertyWithValue("errorCode", ContentErrorCode.INVALID_CURSOR_VALUE);
    }
  }
}
