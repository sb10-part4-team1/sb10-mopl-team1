package com.sb10.mopl.content.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.sb10.mopl.common.storage.ImageStorageService;
import com.sb10.mopl.content.dto.ContentCreateRequest;
import com.sb10.mopl.content.dto.ContentDto;
import com.sb10.mopl.content.dto.ContentUpdateRequest;
import com.sb10.mopl.content.entity.Content;
import com.sb10.mopl.content.entity.ContentType;
import com.sb10.mopl.content.entity.Tag;
import com.sb10.mopl.content.repository.ContentRepository;
import com.sb10.mopl.content.repository.TagRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("ContentService 통합 테스트")
class ContentServiceIntegrationTest {

  @Autowired private ContentService contentService;
  @Autowired private TagRepository tagRepository;
  @Autowired private ContentRepository contentRepository;
  @Autowired private EntityManager em;
  @MockitoBean private ImageStorageService imageStorageService;

  @Test
  @DisplayName("기존 태그와 신규 태그가 혼합되어 있을 때 신규 태그만 실제로 저장된다")
  void createContent_saveOnlyNewTags_whenTagsContainBothExistingAndNewOnes() {
    // given: 기존 태그 "SF"를 저장하고, "SF"와 새로운 태그 "스릴러"를 포함한 요청 준비
    Tag existingTag = tagRepository.save(Tag.create("SF"));

    ContentCreateRequest request =
        new ContentCreateRequest(ContentType.MOVIE, "인셉션", "SF 스릴러 영화", List.of("SF", "스릴러"));

    MockMultipartFile thumbnailFile =
        new MockMultipartFile("thumbnail", "test.jpg", "image/jpeg", "bytes".getBytes());

    when(imageStorageService.upload(thumbnailFile)).thenReturn("/uploads/test.jpg");

    // when: 서비스 레이어의 콘텐츠 생성 기능 호출
    ContentDto result = contentService.createContent(request, thumbnailFile);

    // then: 신규 태그인 "스릴러"만 새로 저장되고 기존 "SF"는 중복 생성되지 않았는지 검증
    assertThat(result).isNotNull();
    assertThat(result.tags()).containsExactlyInAnyOrder("SF", "스릴러");

    // DB에 저장된 전체 태그 개수 검증 ("SF" 1개, "스릴러" 1개 총 2개여야 함)
    List<Tag> allTags = tagRepository.findAll();
    assertThat(allTags).extracting(Tag::getName).containsExactlyInAnyOrder("SF", "스릴러");

    // "SF" 태그의 식별자가 처음에 저장했던 기존 태그의 식별자와 동일한지 검증
    Tag sfTagInDb = tagRepository.findAllByNameIn(List.of("SF")).stream().findFirst().orElseThrow();
    assertThat(sfTagInDb.getId()).isEqualTo(existingTag.getId());

    // 저장된 컨텐츠가 생성되었고 연관된 태그 관계가 정상적으로 설정되었는지 검증
    Content contentInDb = contentRepository.findById(result.id()).orElseThrow();
    assertThat(contentInDb.getContentTags()).hasSize(2);
  }

  @Test
  @DisplayName("태그에 공백이나 중복된 이름이 포함되어 있어도 정규화 및 중복 제거를 통해 유일한 태그들만 영속화된다")
  void createContent_saveNormalizedTagsWithoutDuplicates_whenTagsHaveSpacesOrDuplicates() {
    // given: 공백이 섞이거나 중복된 태그명이 포함된 요청 준비
    ContentCreateRequest request =
        new ContentCreateRequest(
            ContentType.MOVIE, "인터스텔라", "우주 SF 영화", List.of("  SF  ", "SF", "스릴러", "  스릴러  "));

    MockMultipartFile thumbnailFile =
        new MockMultipartFile("thumbnail", "test.jpg", "image/jpeg", "bytes".getBytes());

    when(imageStorageService.upload(thumbnailFile)).thenReturn("/uploads/test.jpg");

    // when: 서비스 레이어의 콘텐츠 생성 기능 호출
    ContentDto result = contentService.createContent(request, thumbnailFile);

    // then: 공백이 제거(trim)되고 중복이 제거된 "SF", "스릴러" 태그만 연관관계가 설정되고 DB에 영속화되는지 확인
    assertThat(result).isNotNull();
    assertThat(result.tags()).containsExactlyInAnyOrder("SF", "스릴러");

    // DB에 저장된 태그가 중복 없이 각각 1개씩 총 2개만 존재하는지 검증
    List<Tag> allTags = tagRepository.findAll();
    assertThat(allTags).extracting(Tag::getName).containsExactlyInAnyOrder("SF", "스릴러");
  }

  @Test
  @DisplayName("콘텐츠 정보 수정 시 일반 필드와 태그 연관관계가 DB 레벨에서 정상 동기화 및 갱신된다")
  void updateContent_success_whenDataIsValid() {
    // given: 기존 태그 "SF"와 콘텐츠 저장 후, 다른 태그 조합과 새 세부사항 설정
    Tag existingSfTag = tagRepository.save(Tag.create("SF"));
    ContentCreateRequest createRequest =
        new ContentCreateRequest(ContentType.MOVIE, "인셉션", "SF 영화", List.of("SF"));
    MockMultipartFile thumbnailFile =
        new MockMultipartFile("thumbnail", "test.jpg", "image/jpeg", "bytes".getBytes());
    when(imageStorageService.upload(thumbnailFile)).thenReturn("/uploads/test.jpg");
    ContentDto created = contentService.createContent(createRequest, thumbnailFile);

    ContentUpdateRequest updateRequest =
        new ContentUpdateRequest("인셉션 수정", "SF 영화 수정", List.of("SF", "액션"));
    MockMultipartFile updateThumbnailFile =
        new MockMultipartFile("thumbnail", "updated.jpg", "image/jpeg", "bytes".getBytes());
    when(imageStorageService.upload(updateThumbnailFile)).thenReturn("/uploads/updated.jpg");

    // when: 서비스 레이어 콘텐츠 수정 호출
    ContentDto updated =
        contentService.updateContent(created.id(), updateRequest, updateThumbnailFile);

    // then: 필드 수정 및 태그 동기화(SF 유지, 액션 추가) 완료 검증
    assertThat(updated).isNotNull();
    assertThat(updated.title()).isEqualTo("인셉션 수정");
    assertThat(updated.thumbnailUrl()).isEqualTo("/uploads/updated.jpg");
    assertThat(updated.tags()).containsExactlyInAnyOrder("SF", "액션");

    // DB 상에 content_tags 매핑도 SF와 액션 단 2개만 맺어져 있는지 검증
    Content contentInDb = contentRepository.findById(updated.id()).orElseThrow();
    assertThat(contentInDb.getContentTags()).hasSize(2);

    // DB에 저장된 전체 태그 개수 검증 ("SF" 1개, "액션" 1개 총 2개여야 하며, 중복 생성되지 않았는지 확인)
    List<Tag> allTags = tagRepository.findAll();
    assertThat(allTags).extracting(Tag::getName).containsExactlyInAnyOrder("SF", "액션");

    // "SF" 태그의 식별자가 처음에 저장했던 기존 태그의 식별자와 동일한지 검증 (기존 태그 재사용 여부 확인)
    Tag sfTagInDb = tagRepository.findAllByNameIn(List.of("SF")).stream().findFirst().orElseThrow();
    assertThat(sfTagInDb.getId()).isEqualTo(existingSfTag.getId());
  }

  @Test
  @DisplayName("DB 상에서 콘텐츠의 리뷰 통계 및 실시간 시청자 수 필드가 정상 반영된다")
  void updateStatisticsAndWatcherCount_success_whenDataIsValid() {
    // given: 새로운 콘텐츠 생성
    ContentCreateRequest createRequest =
        new ContentCreateRequest(ContentType.MOVIE, "인셉션", "SF 영화", List.of("SF"));
    MockMultipartFile thumbnailFile =
        new MockMultipartFile("thumbnail", "test.jpg", "image/jpeg", "bytes".getBytes());
    when(imageStorageService.upload(thumbnailFile)).thenReturn("/uploads/test.jpg");
    ContentDto created = contentService.createContent(createRequest, thumbnailFile);

    // when: 통계 갱신 및 시청자 수 갱신 수행
    contentService.updateStatistics(created.id(), 4.2, 24);
    ContentDto updatedDto = contentService.updateWatcherCount(created.id(), 750L);

    // then: DB에 해당 수치가 정상 영속화되었는지 검증
    assertThat(updatedDto.averageRating()).isEqualTo(4.2);
    assertThat(updatedDto.reviewCount()).isEqualTo(24);
    assertThat(updatedDto.watcherCount()).isEqualTo(750L);

    Content contentInDb = contentRepository.findById(created.id()).orElseThrow();
    assertThat(contentInDb.getAverageRating()).isEqualTo(4.2);
    assertThat(contentInDb.getReviewCount()).isEqualTo(24);
    assertThat(contentInDb.getWatcherCount()).isEqualTo(750L);
  }

  @Test
  @DisplayName("콘텐츠 삭제 시 연관된 매핑 데이터(태그 매핑 등)도 DB 레벨에서 CASCADE로 함께 연쇄 삭제된다")
  void deleteContent_successAndCascadesDeleted_whenDataIsValid() {
    // given: 태그를 포함한 콘텐츠를 생성 및 저장
    ContentCreateRequest createRequest =
        new ContentCreateRequest(ContentType.MOVIE, "인셉션", "SF 영화", List.of("SF", "스릴러"));
    MockMultipartFile thumbnailFile =
        new MockMultipartFile("thumbnail", "test.jpg", "image/jpeg", "bytes".getBytes());
    when(imageStorageService.upload(thumbnailFile)).thenReturn("/uploads/test.jpg");

    ContentDto created = contentService.createContent(createRequest, thumbnailFile);

    // 태그 테이블에 태그가 정상 저장되었고 관계도 맺어졌는지 사전 검증
    assertThat(tagRepository.findAllByNameIn(List.of("SF", "스릴러"))).hasSize(2);

    // when: 콘텐츠 삭제 로직 호출
    contentService.deleteContent(created.id());

    // then: 콘텐츠가 DB에서 정상적으로 삭제되었는지 확인
    assertThat(contentRepository.findById(created.id())).isEmpty();

    // DB 상에 content_tags 매핑도 CASCADE로 함께 연쇄 삭제되었는지 검증
    // (JPA 연관관계 lazy loading 검증을 피하기 위해 JPQL로 DB에 데이터가 완전히 없는지 직접 조회)
    java.util.List<?> contentTags =
        em.createQuery("select ct from ContentTag ct where ct.content.id = :contentId")
            .setParameter("contentId", created.id())
            .getResultList();
    assertThat(contentTags).isEmpty();
  }
}
