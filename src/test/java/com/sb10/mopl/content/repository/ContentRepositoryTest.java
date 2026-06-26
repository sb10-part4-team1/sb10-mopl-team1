package com.sb10.mopl.content.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.sb10.mopl.common.pagination.SortDirection;
import com.sb10.mopl.config.JpaAuditingConfig;
import com.sb10.mopl.config.QuerydslConfig;
import com.sb10.mopl.content.dto.ContentSearchRequest;
import com.sb10.mopl.content.dto.ContentSortBy;
import com.sb10.mopl.content.entity.Content;
import com.sb10.mopl.content.entity.ContentTag;
import com.sb10.mopl.content.entity.ContentType;
import com.sb10.mopl.content.entity.Tag;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaAuditingConfig.class, QuerydslConfig.class})
class ContentRepositoryTest {

  @Autowired private ContentRepository contentRepository;
  @Autowired private TagRepository tagRepository;
  @Autowired private EntityManager em;

  @Test
  @DisplayName("콘텐츠 등록이 정상적으로 완료된다")
  void save_success_whenContentDataIsValid() {
    // given: 올바른 콘텐츠 엔티티를 빌드
    Content content =
        Content.create("인셉션", ContentType.MOVIE, "SF 스릴러 영화", "/uploads/inception.jpg");

    // when: 콘텐츠 영속화 수행
    Content savedContent = contentRepository.save(content);

    // then: 식별자 UUID가 자동 할당되었고 올바르게 저장되었는지 확인
    assertThat(savedContent.getId()).isNotNull();
    assertThat(savedContent.getTitle()).isEqualTo("인셉션");
    assertThat(savedContent.getType()).isEqualTo(ContentType.MOVIE);
  }

  @Test
  @DisplayName("목록 조회 시 조건 필터링, 다양한 정렬 기준 및 커서 페이지네이션이 정상 작동한다")
  void findAllByCondition_success_withFiltersAndSortingAndCursor() {
    // given: 테스트용 콘텐츠 데이터 및 태그 직접 관계 설정 저장
    Tag sf = tagRepository.save(Tag.create("SF"));

    Content contentA = Content.create("인셉션", ContentType.MOVIE, "SF 영화", "/uploads/test.jpg");
    ContentTag.create(contentA, sf);
    contentRepository.save(contentA);
    contentA.updateStatistics(4.5, 10);
    contentA.updateWatcherCount(300L);

    Tag action = tagRepository.save(Tag.create("액션"));
    Content contentB = Content.create("인터스텔라", ContentType.MOVIE, "우주 SF 영화", "/uploads/test.jpg");
    ContentTag.create(contentB, sf);
    ContentTag.create(contentB, action);
    contentRepository.save(contentB);
    contentB.updateStatistics(4.0, 5);
    contentB.updateWatcherCount(500L);

    Tag drama = tagRepository.save(Tag.create("드라마"));
    Content contentC =
        Content.create("시그널", ContentType.TV_SERIES, "타임슬립 드라마", "/uploads/test.jpg");
    ContentTag.create(contentC, drama);
    contentRepository.save(contentC);
    contentC.updateStatistics(4.8, 20);
    contentC.updateWatcherCount(100L);

    em.flush();
    em.clear();

    // 1) 필터 테스트: typeEqual = MOVIE
    ContentSearchRequest reqType =
        new ContentSearchRequest(
            ContentType.MOVIE,
            null,
            null,
            null,
            null,
            10,
            SortDirection.DESCENDING,
            ContentSortBy.createdAt);
    List<Content> resType = contentRepository.findAllByCondition(reqType);
    assertThat(resType).hasSize(2);
    assertThat(resType).extracting(Content::getTitle).containsExactlyInAnyOrder("인셉션", "인터스텔라");
    assertThat(contentRepository.countContents(reqType)).isEqualTo(2L);

    // 2) 필터 테스트: keywordLike = "영화"
    ContentSearchRequest reqKeyword =
        new ContentSearchRequest(
            null, "영화", null, null, null, 10, SortDirection.DESCENDING, ContentSortBy.createdAt);
    List<Content> resKeyword = contentRepository.findAllByCondition(reqKeyword);
    assertThat(resKeyword).hasSize(2);
    assertThat(resKeyword).extracting(Content::getTitle).containsExactlyInAnyOrder("인셉션", "인터스텔라");
    assertThat(contentRepository.countContents(reqKeyword)).isEqualTo(2L);

    // 3) 필터 테스트: tagsIn = List.of("액션", "드라마")
    ContentSearchRequest reqTags =
        new ContentSearchRequest(
            null,
            null,
            List.of("액션", "드라마"),
            null,
            null,
            10,
            SortDirection.DESCENDING,
            ContentSortBy.createdAt);
    List<Content> resTags = contentRepository.findAllByCondition(reqTags);
    assertThat(resTags).hasSize(2);
    assertThat(resTags).extracting(Content::getTitle).containsExactlyInAnyOrder("인터스텔라", "시그널");
    assertThat(contentRepository.countContents(reqTags)).isEqualTo(2L);

    // 4) 정렬 및 커서 페이징 테스트: sortBy = POPULAR (인기순: watcherCount desc, reviewCount desc, id desc)
    // 순서: B(500) -> A(300) -> C(100)
    // limit = 2로 조회 시 다음 페이지 여부를 알기 위해 3개 조회
    ContentSearchRequest reqPopularPage1 =
        new ContentSearchRequest(
            null, null, null, null, null, 2, SortDirection.DESCENDING, ContentSortBy.watcherCount);
    List<Content> resPopularPage1 = contentRepository.findAllByCondition(reqPopularPage1);
    assertThat(resPopularPage1).hasSize(3);
    assertThat(resPopularPage1.get(0).getTitle()).isEqualTo("인터스텔라");
    assertThat(resPopularPage1.get(1).getTitle()).isEqualTo("인셉션");
    assertThat(resPopularPage1.get(2).getTitle()).isEqualTo("시그널");

    // 슬라이스 처리된 목록의 마지막 아이템: 인셉션 (A)
    List<Content> page1 = resPopularPage1.subList(0, 2);
    Content lastItem = page1.get(1);

    // nextIdAfter를 idAfter로 하여 두 번째 페이지 조회
    ContentSearchRequest reqPopularPage2 =
        new ContentSearchRequest(
            null,
            null,
            null,
            lastItem.getWatcherCount() + "_" + lastItem.getReviewCount(),
            lastItem.getId(),
            2,
            SortDirection.DESCENDING,
            ContentSortBy.watcherCount);
    List<Content> resPopularPage2 = contentRepository.findAllByCondition(reqPopularPage2);
    assertThat(resPopularPage2).hasSize(1);
    assertThat(resPopularPage2.get(0).getTitle()).isEqualTo("시그널");
  }

  @Test
  @DisplayName("목록 조회 시 생성일 오름차순(CREATED_AT ASC) 정렬 및 커서 페이징이 정상 작동한다")
  void findAllByCondition_success_withCreatedAtSortAndAscending() {
    // given: 생성 순서대로 A, B, C 저장 (createdAt 순서: A < B < C)
    Content contentA = Content.create("인셉션", ContentType.MOVIE, "SF 영화", "/uploads/test.jpg");
    contentRepository.save(contentA);
    ReflectionTestUtils.setField(contentA, "createdAt", Instant.now().minusSeconds(10));

    Content contentB = Content.create("인터스텔라", ContentType.MOVIE, "우주 SF 영화", "/uploads/test.jpg");
    contentRepository.save(contentB);
    ReflectionTestUtils.setField(contentB, "createdAt", Instant.now().minusSeconds(5));

    Content contentC =
        Content.create("시그널", ContentType.TV_SERIES, "타임슬립 드라마", "/uploads/test.jpg");
    contentRepository.save(contentC);
    ReflectionTestUtils.setField(contentC, "createdAt", Instant.now());

    em.flush();
    em.clear();

    // 첫 페이지 조회 (limit = 2) -> 오름차순이므로 A -> B -> C 순서. A, B가 첫 페이지
    ContentSearchRequest reqPage1 =
        new ContentSearchRequest(
            null, null, null, null, null, 2, SortDirection.ASCENDING, ContentSortBy.createdAt);
    List<Content> resPage1 = contentRepository.findAllByCondition(reqPage1);
    assertThat(resPage1).hasSize(3); // hasNext를 위해 3개 로드
    assertThat(resPage1.get(0).getTitle()).isEqualTo("인셉션");
    assertThat(resPage1.get(1).getTitle()).isEqualTo("인터스텔라");

    // 슬라이스된 첫 페이지의 마지막 아이템: B (인터스텔라)
    Content lastItem = resPage1.get(1);

    // 두 번째 페이지 조회 -> C (시그널)가 조회되어야 함
    ContentSearchRequest reqPage2 =
        new ContentSearchRequest(
            null,
            null,
            null,
            lastItem.getCreatedAt().toString(),
            lastItem.getId(),
            2,
            SortDirection.ASCENDING,
            ContentSortBy.createdAt);
    List<Content> resPage2 = contentRepository.findAllByCondition(reqPage2);
    assertThat(resPage2).hasSize(1);
    assertThat(resPage2.get(0).getTitle()).isEqualTo("시그널");
  }

  @Test
  @DisplayName("목록 조회 시 평점 내림차순(RATING DESC) 정렬 및 커서 페이징이 정상 작동한다")
  void findAllByCondition_success_withRatingSortAndDescending() {
    // given: 평점을 다르게 부여한 A(4.5), B(4.0), C(4.8) 저장
    Content contentA = Content.create("인셉션", ContentType.MOVIE, "SF 영화", "/uploads/test.jpg");
    contentRepository.save(contentA);
    contentA.updateStatistics(4.5, 10);

    Content contentB = Content.create("인터스텔라", ContentType.MOVIE, "우주 SF 영화", "/uploads/test.jpg");
    contentRepository.save(contentB);
    contentB.updateStatistics(4.0, 5);

    Content contentC =
        Content.create("시그널", ContentType.TV_SERIES, "타임슬립 드라마", "/uploads/test.jpg");
    contentRepository.save(contentC);
    contentC.updateStatistics(4.8, 20);

    em.flush();
    em.clear();

    // 첫 페이지 조회 (limit = 2) -> 내림차순 정렬: C(4.8) -> A(4.5) -> B(4.0) 순서. C, A가 첫 페이지
    ContentSearchRequest reqPage1 =
        new ContentSearchRequest(
            null, null, null, null, null, 2, SortDirection.DESCENDING, ContentSortBy.rate);
    List<Content> resPage1 = contentRepository.findAllByCondition(reqPage1);
    assertThat(resPage1).hasSize(3); // hasNext를 위해 3개 로드
    assertThat(resPage1.get(0).getTitle()).isEqualTo("시그널");
    assertThat(resPage1.get(1).getTitle()).isEqualTo("인셉션");

    // 첫 페이지의 마지막 아이템: A (인셉션)
    Content lastItem = resPage1.get(1);

    // 두 번째 페이지 조회 -> B (인터스텔라)가 조회되어야 함
    ContentSearchRequest reqPage2 =
        new ContentSearchRequest(
            null,
            null,
            null,
            String.valueOf(lastItem.getAverageRating()),
            lastItem.getId(),
            2,
            SortDirection.DESCENDING,
            ContentSortBy.rate);
    List<Content> resPage2 = contentRepository.findAllByCondition(reqPage2);
    assertThat(resPage2).hasSize(1);
    assertThat(resPage2.get(0).getTitle()).isEqualTo("인터스텔라");
  }
}
