package com.sb10.mopl.content.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.sb10.mopl.common.storage.ImageStorageService;
import com.sb10.mopl.content.dto.ContentCreateRequest;
import com.sb10.mopl.content.dto.ContentDto;
import com.sb10.mopl.content.entity.Content;
import com.sb10.mopl.content.entity.ContentType;
import com.sb10.mopl.content.entity.Tag;
import com.sb10.mopl.content.repository.ContentRepository;
import com.sb10.mopl.content.repository.TagRepository;
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
}
