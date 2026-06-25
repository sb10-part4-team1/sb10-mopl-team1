package com.sb10.mopl.content.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sb10.mopl.common.storage.ImageStorageService;
import com.sb10.mopl.content.dto.ContentCreateRequest;
import com.sb10.mopl.content.dto.ContentDto;
import com.sb10.mopl.content.entity.Content;
import com.sb10.mopl.content.entity.ContentType;
import com.sb10.mopl.content.entity.Tag;
import com.sb10.mopl.content.mapper.ContentMapper;
import com.sb10.mopl.content.repository.ContentRepository;
import com.sb10.mopl.content.repository.TagRepository;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class ContentServiceTest {

  private final ContentMapper contentMapper = Mappers.getMapper(ContentMapper.class);
  private ContentService contentService;
  @Mock private ContentRepository contentRepository;
  @Mock private TagRepository tagRepository;
  @Mock private ImageStorageService imageStorageService;

  @BeforeEach
  void setUp() {
    contentService =
        new ContentService(contentRepository, tagRepository, contentMapper, imageStorageService);
  }

  @Test
  @DisplayName("콘텐츠 정보가 유효하고 모든 태그가 신규이면 콘텐츠 생성에 성공한다")
  void createContent_success_whenDataIsValidAndTagsAreNew() {
    // given: DTO, Mock 파일 준비 및 Repository 동작 목 스터빙
    ContentCreateRequest request =
        new ContentCreateRequest(ContentType.MOVIE, "인셉션", "SF 영화", List.of("SF", "스릴러"));

    MockMultipartFile thumbnailFile =
        new MockMultipartFile("thumbnail", "test.jpg", "image/jpeg", "bytes".getBytes());

    when(imageStorageService.upload(thumbnailFile)).thenReturn("/uploads/test.jpg");
    when(tagRepository.findAllByNameIn(anyCollection())).thenReturn(Collections.emptyList());

    Tag sfTag = Tag.create("SF");
    Tag thrillerTag = Tag.create("스릴러");
    when(tagRepository.saveAll(anyCollection())).thenReturn(List.of(sfTag, thrillerTag));

    when(contentRepository.save(any(Content.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // when: 서비스 레이어의 콘텐츠 생성 기능 실행
    ContentDto result = contentService.createContent(request, thumbnailFile);

    // then: 저장 로직이 정상 처리되었고 DTO 응답 스펙이 알맞게 리턴되었는지 확인
    assertThat(result).isNotNull();
    assertThat(result.type()).isEqualTo("MOVIE");
    assertThat(result.title()).isEqualTo("인셉션");
    assertThat(result.thumbnailUrl()).isEqualTo("/uploads/test.jpg");
    assertThat(result.tags()).containsExactlyInAnyOrder("SF", "스릴러");
    verify(tagRepository).saveAll(anyCollection());
  }

  @Test
  @DisplayName("썸네일 업로드 장애가 발생하여 null이 반환되면 디폴트 썸네일 경로를 제공하며 생성을 수행한다")
  void createContent_successWithFallback_whenThumbnailUploadReturnsNull() {
    // given: 썸네일 업로드 기능이 실패(null 리턴)하는 목 시나리오 설정
    ContentCreateRequest request =
        new ContentCreateRequest(ContentType.SPORT, "축구 중계", "하이라이트", Collections.emptyList());

    MockMultipartFile thumbnailFile =
        new MockMultipartFile("thumbnail", "test.jpg", "image/jpeg", "bytes".getBytes());

    when(imageStorageService.upload(thumbnailFile)).thenReturn(null);
    when(contentRepository.save(any(Content.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // when: 서비스 레이어 호출
    ContentDto result = contentService.createContent(request, thumbnailFile);

    // then: 업로드 실패에도 디폴트 이미지 경로인 "/uploads/default-thumbnail.png"로 세팅되는지 확인
    assertThat(result).isNotNull();
    assertThat(result.type()).isEqualTo("SPORT");
    assertThat(result.title()).isEqualTo("축구 중계");
    assertThat(result.thumbnailUrl()).isEqualTo("/uploads/default-thumbnail.png");
    verify(tagRepository, never()).saveAll(anyCollection());
  }

  @Test
  @DisplayName("썸네일 파일이 null이면 기본 썸네일 경로를 사용하여 콘텐츠 생성에 성공한다")
  void createContent_successWithFallback_whenThumbnailIsNull() {
    // given: 썸네일이 null인 요청 구성
    ContentCreateRequest request =
        new ContentCreateRequest(ContentType.SPORT, "축구 중계", "하이라이트", Collections.emptyList());

    when(contentRepository.save(any(Content.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // when: 서비스 레이어 호출
    ContentDto result = contentService.createContent(request, null);

    // then: 기본 썸네일 경로 "/uploads/default-thumbnail.png"로 세팅되는지 확인
    assertThat(result).isNotNull();
    assertThat(result.thumbnailUrl()).isEqualTo("/uploads/default-thumbnail.png");
  }

  @Test
  @DisplayName("썸네일 파일이 비어 있으면 기본 썸네일 경로를 사용하여 콘텐츠 생성에 성공한다")
  void createContent_successWithFallback_whenThumbnailIsEmpty() {
    // given: 빈 썸네일 파일 요청 구성
    ContentCreateRequest request =
        new ContentCreateRequest(ContentType.SPORT, "축구 중계", "하이라이트", Collections.emptyList());

    MockMultipartFile emptyThumbnail =
        new MockMultipartFile("thumbnail", "", "image/jpeg", new byte[0]);

    when(contentRepository.save(any(Content.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // when: 서비스 레이어 호출
    ContentDto result = contentService.createContent(request, emptyThumbnail);

    // then: 기본 썸네일 경로 "/uploads/default-thumbnail.png"로 세팅되는지 확인
    assertThat(result).isNotNull();
    assertThat(result.thumbnailUrl()).isEqualTo("/uploads/default-thumbnail.png");
  }

  @Test
  @SuppressWarnings("unchecked")
  @DisplayName("이미 존재하는 태그와 신규 태그가 섞여 있을 때 신규 태그만 저장하고 기존 태그는 조회해서 사용한다")
  void createContent_successWithMixedTags_whenSomeTagsAlreadyExist() {
    // given: "SF" 태그는 이미 존재하고, "스릴러" 태그는 새로운 태그인 상황 준비
    ContentCreateRequest request =
        new ContentCreateRequest(ContentType.MOVIE, "인터스텔라", "우주 SF 영화", List.of("SF", "스릴러"));

    MockMultipartFile thumbnailFile =
        new MockMultipartFile("thumbnail", "test.jpg", "image/jpeg", "bytes".getBytes());

    when(imageStorageService.upload(thumbnailFile)).thenReturn("/uploads/test.jpg");

    Tag existingSfTag = Tag.create("SF");
    when(tagRepository.findAllByNameIn(anyCollection())).thenReturn(List.of(existingSfTag));

    Tag newThrillerTag = Tag.create("스릴러");
    when(tagRepository.saveAll(anyCollection())).thenReturn(List.of(newThrillerTag));

    when(contentRepository.save(any(Content.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // when: 서비스 레이어 콘텐츠 생성 수행
    ContentDto result = contentService.createContent(request, thumbnailFile);

    // then: 신규 태그인 "스릴러"만 saveAll 되었는지 확인하고, DTO에 두 태그명이 모두 포함되었는지 검증
    assertThat(result).isNotNull();
    assertThat(result.tags()).containsExactlyInAnyOrder("SF", "스릴러");

    org.mockito.ArgumentCaptor<List<Tag>> tagCaptor =
        org.mockito.ArgumentCaptor.forClass(List.class);
    verify(tagRepository).saveAll(tagCaptor.capture());
    List<Tag> capturedNewTags = tagCaptor.getValue();

    assertThat(capturedNewTags).hasSize(1);
    assertThat(capturedNewTags.get(0).getName()).isEqualTo("스릴러");
  }
}
