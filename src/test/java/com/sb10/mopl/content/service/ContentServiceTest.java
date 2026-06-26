package com.sb10.mopl.content.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sb10.mopl.common.pagination.CursorPageResponse;
import com.sb10.mopl.common.pagination.SortDirection;
import com.sb10.mopl.common.storage.ImageStorageService;
import com.sb10.mopl.content.dto.ContentCreateRequest;
import com.sb10.mopl.content.dto.ContentDto;
import com.sb10.mopl.content.dto.ContentSearchRequest;
import com.sb10.mopl.content.dto.ContentSortBy;
import com.sb10.mopl.content.dto.ContentUpdateRequest;
import com.sb10.mopl.content.entity.Content;
import com.sb10.mopl.content.entity.ContentType;
import com.sb10.mopl.content.entity.Tag;
import com.sb10.mopl.content.exception.ContentErrorCode;
import com.sb10.mopl.content.exception.ContentException;
import com.sb10.mopl.content.mapper.ContentMapper;
import com.sb10.mopl.content.repository.ContentRepository;
import com.sb10.mopl.content.repository.TagRepository;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

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
    ContentDto result = contentService.create(request, thumbnailFile);

    // then: 저장 로직이 정상 처리되었고 DTO 응답 스펙이 알맞게 리턴되었는지 확인
    assertThat(result).isNotNull();
    assertThat(result.type()).isEqualTo("MOVIE");
    assertThat(result.title()).isEqualTo("인셉션");
    assertThat(result.thumbnailUrl()).isEqualTo("/uploads/test.jpg");
    assertThat(result.tags()).containsExactlyInAnyOrder("SF", "스릴러");
    verify(tagRepository).saveAll(anyCollection());
    verify(contentRepository).save(any(Content.class));
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
    ContentDto result = contentService.create(request, thumbnailFile);

    // then: 업로드 실패에도 디폴트 이미지 경로인 "/uploads/default-thumbnail.png"로 세팅되는지 확인
    assertThat(result).isNotNull();
    assertThat(result.type()).isEqualTo("SPORT");
    assertThat(result.title()).isEqualTo("축구 중계");
    assertThat(result.thumbnailUrl()).isEqualTo("/uploads/default-thumbnail.png");
    verify(tagRepository, never()).saveAll(anyCollection());
    verify(contentRepository).save(any(Content.class));
  }

  @Test
  @DisplayName("썸네일 업로드 결과가 공백 문자열인 경우 디폴트 썸네일 경로를 제공하며 생성을 수행한다")
  void createContent_successWithFallback_whenThumbnailUploadReturnsBlank() {
    // given: 썸네일 업로드 결과가 공백("   ")을 리턴하는 목 시나리오 설정
    ContentCreateRequest request =
        new ContentCreateRequest(ContentType.SPORT, "축구 중계", "하이라이트", Collections.emptyList());

    MockMultipartFile thumbnailFile =
        new MockMultipartFile("thumbnail", "test.jpg", "image/jpeg", "bytes".getBytes());

    when(imageStorageService.upload(thumbnailFile)).thenReturn("   ");
    when(contentRepository.save(any(Content.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // when: 서비스 레이어 호출
    ContentDto result = contentService.create(request, thumbnailFile);

    // then: 업로드 결과가 공백이어도 디폴트 이미지 경로인 "/uploads/default-thumbnail.png"로 세팅되는지 확인
    assertThat(result).isNotNull();
    assertThat(result.type()).isEqualTo("SPORT");
    assertThat(result.title()).isEqualTo("축구 중계");
    assertThat(result.thumbnailUrl()).isEqualTo("/uploads/default-thumbnail.png");
    verify(tagRepository, never()).saveAll(anyCollection());
    verify(contentRepository).save(any(Content.class));
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
    ContentDto result = contentService.create(request, null);

    // then: 기본 썸네일 경로 "/uploads/default-thumbnail.png"로 세팅되는지 확인
    assertThat(result).isNotNull();
    assertThat(result.thumbnailUrl()).isEqualTo("/uploads/default-thumbnail.png");
    verify(contentRepository).save(any(Content.class));
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
    ContentDto result = contentService.create(request, emptyThumbnail);

    // then: 기본 썸네일 경로 "/uploads/default-thumbnail.png"로 세팅되는지 확인
    assertThat(result).isNotNull();
    assertThat(result.thumbnailUrl()).isEqualTo("/uploads/default-thumbnail.png");
    verify(contentRepository).save(any(Content.class));
  }

  @Test
  @DisplayName("콘텐츠 정보가 유효하면 콘텐츠 일반 필드 및 태그 수정에 성공한다")
  void updateContent_success_whenDataIsValid() {
    // given: 수정 요청 DTO, 기존 콘텐츠 엔티티 모킹 설정
    UUID contentId = UUID.randomUUID();
    Content content = Content.create("인셉션", ContentType.MOVIE, "SF 영화", "/uploads/test.jpg");

    ContentUpdateRequest request =
        new ContentUpdateRequest("인셉션 수정", "SF 영화 수정", List.of("SF", "액션"));
    MockMultipartFile thumbnailFile =
        new MockMultipartFile("thumbnail", "updated.jpg", "image/jpeg", "bytes".getBytes());

    when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));
    when(imageStorageService.upload(thumbnailFile)).thenReturn("/uploads/updated.jpg");

    Tag sfTag = Tag.create("SF");
    Tag actionTag = Tag.create("액션");
    when(tagRepository.findAllByNameIn(anyCollection())).thenReturn(List.of(sfTag));
    when(tagRepository.saveAll(anyCollection())).thenReturn(List.of(actionTag));

    // when: 서비스 레이어의 콘텐츠 수정 기능 실행
    ContentDto result = contentService.update(contentId, request, thumbnailFile);

    // then: 필드 수정 및 태그 정보가 정상적으로 갱신되었는지 확인
    assertThat(result).isNotNull();
    assertThat(result.title()).isEqualTo("인셉션 수정");
    assertThat(result.description()).isEqualTo("SF 영화 수정");
    assertThat(result.thumbnailUrl()).isEqualTo("/uploads/updated.jpg");
    assertThat(result.tags()).containsExactlyInAnyOrder("SF", "액션");
  }

  @Test
  @DisplayName("콘텐츠 수정 요청 시 썸네일 파일이 비어 있으면 기존 썸네일을 유지한다")
  void updateContent_successWithKeepThumbnail_whenThumbnailFileIsEmpty() {
    // given: 빈 썸네일 파일을 포함한 수정 요청 구성
    UUID contentId = UUID.randomUUID();
    Content content = Content.create("인셉션", ContentType.MOVIE, "SF 영화", "/uploads/test.jpg");

    ContentUpdateRequest request =
        new ContentUpdateRequest("인셉션 수정", "SF 영화 수정", Collections.emptyList());
    MockMultipartFile emptyThumbnail =
        new MockMultipartFile("thumbnail", "", "image/jpeg", new byte[0]);

    when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));

    // when: 서비스 레이어 콘텐츠 수정 실행
    ContentDto result = contentService.update(contentId, request, emptyThumbnail);

    // then: 제목/설명은 바뀌고 썸네일은 기존 "/uploads/test.jpg"로 유지되었는지 확인
    assertThat(result).isNotNull();
    assertThat(result.title()).isEqualTo("인셉션 수정");
    assertThat(result.thumbnailUrl()).isEqualTo("/uploads/test.jpg");
  }

  @Test
  @DisplayName("존재하지 않는 콘텐츠 ID로 수정 요청 시 CONTENT_NOT_FOUND 예외가 발생한다")
  void updateContent_fail_whenContentNotFound() {
    // given: 존재하지 않는 임의의 ID 구성
    UUID invalidId = UUID.randomUUID();
    ContentUpdateRequest request =
        new ContentUpdateRequest("인셉션 수정", "SF 영화 수정", Collections.emptyList());

    when(contentRepository.findById(invalidId)).thenReturn(Optional.empty());

    // when & then: 수정 실행 시 ContentException 던져지는지 확인
    Assertions.assertThrows(
        ContentException.class, () -> contentService.update(invalidId, request, null));
  }

  @Test
  @DisplayName("리뷰 수 및 평점 업데이트에 성공한다")
  void updateStatistics_success_whenDataIsValid() {
    // given: 콘텐츠 및 통계 수정 값 설정
    UUID contentId = UUID.randomUUID();
    Content content = Content.create("인셉션", ContentType.MOVIE, "SF 영화", "/uploads/test.jpg");

    when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));

    // when: 통계 업데이트 로직 호출
    ContentDto result = contentService.updateStatistics(contentId, 4.5, 10);

    // then: 평점과 리뷰 수가 올바르게 갱신되었는지 검증
    assertThat(result.averageRating()).isEqualTo(4.5);
    assertThat(result.reviewCount()).isEqualTo(10);
  }

  @Test
  @DisplayName("평점 범위를 초과하는 통계 업데이트 요청 시 예외가 발생한다")
  void updateStatistics_fail_whenAverageRatingIsInvalid() {
    // given: 범위를 초과하는 평점(5.5) 설정
    UUID contentId = UUID.randomUUID();
    Content content = Content.create("인셉션", ContentType.MOVIE, "SF 영화", "/uploads/test.jpg");

    when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));

    // when & then: 평점 범위 위반 예외 검증
    Assertions.assertThrows(
        ContentException.class, () -> contentService.updateStatistics(contentId, 5.5, 10));
  }

  @Test
  @DisplayName("시청자 수 업데이트에 성공한다")
  void updateWatcherCount_success_whenDataIsValid() {
    // given: 콘텐츠 및 시청자 수 설정
    UUID contentId = UUID.randomUUID();
    Content content = Content.create("인셉션", ContentType.MOVIE, "SF 영화", "/uploads/test.jpg");

    when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));

    // when: 시청자 수 업데이트 호출
    ContentDto result = contentService.updateWatcherCount(contentId, 1500L);

    // then: 시청자 수가 정상적으로 반영되었는지 검증
    assertThat(result.watcherCount()).isEqualTo(1500L);
  }

  @Test
  @DisplayName("시청자 수가 음수이면 업데이트 시 예외가 발생한다")
  void updateWatcherCount_fail_whenWatcherCountIsNegative() {
    // given: 음수 시청자 수 설정
    UUID contentId = UUID.randomUUID();
    Content content = Content.create("인셉션", ContentType.MOVIE, "SF 영화", "/uploads/test.jpg");

    when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));

    // when & then: 음수값 위반 예외 검증
    Assertions.assertThrows(
        ContentException.class, () -> contentService.updateWatcherCount(contentId, -1L));
  }

  @Test
  @DisplayName("태그 저장 중 동시성 제약 조건 예외가 발생하면 DUPLICATE_TAG_NAME 예외가 발생한다")
  void createContent_fail_whenDataIntegrityViolationOccursOnTagSave() {
    // given: DTO, Mock 파일 준비 및 tagRepository.saveAll이 DataIntegrityViolationException을 발생시키도록 목 스터빙
    ContentCreateRequest request =
        new ContentCreateRequest(ContentType.MOVIE, "인셉션", "SF 영화", List.of("SF"));
    MockMultipartFile thumbnailFile =
        new MockMultipartFile("thumbnail", "test.jpg", "image/jpeg", "bytes".getBytes());

    when(imageStorageService.upload(thumbnailFile)).thenReturn("/uploads/test.jpg");
    when(tagRepository.findAllByNameIn(anyCollection())).thenReturn(Collections.emptyList());
    when(tagRepository.saveAll(anyCollection()))
        .thenThrow(new DataIntegrityViolationException("Duplicate key"));

    // when & then: 콘텐츠 생성 시 ContentException이 발생하는지 검증하고, 에러 코드가 DUPLICATE_TAG_NAME인지 확인
    ContentException exception =
        Assertions.assertThrows(
            ContentException.class, () -> contentService.create(request, thumbnailFile));
    assertThat(exception.getErrorCode()).isEqualTo(ContentErrorCode.DUPLICATE_TAG_NAME);
    verify(contentRepository, never()).save(any(Content.class));
  }

  @Test
  @DisplayName("존재하는 콘텐츠 ID로 삭제 요청 시 삭제에 성공한다")
  void deleteContent_success_whenIdIsValid() {
    // given: 존재하는 임의의 콘텐츠 ID 및 조회 모킹 설정
    UUID contentId = UUID.randomUUID();
    Content content = Content.create("인셉션", ContentType.MOVIE, "SF 영화", "/uploads/test.jpg");
    when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));

    // when: 서비스 레이어 삭제 호출
    contentService.delete(contentId);

    // then: repository의 delete 호출 확인
    verify(contentRepository).delete(content);
  }

  @Test
  @DisplayName("존재하지 않는 콘텐츠 ID로 삭제 요청 시 CONTENT_NOT_FOUND 예외가 발생한다")
  void deleteContent_fail_whenIdIsInvalid() {
    // given: 존재하지 않는 임의의 ID 구성
    UUID invalidId = UUID.randomUUID();
    when(contentRepository.findById(invalidId)).thenReturn(Optional.empty());

    // when & then: 삭제 호출 시 ContentException이 발생하는지 및 에러 코드가 CONTENT_NOT_FOUND인지 확인
    ContentException exception =
        Assertions.assertThrows(ContentException.class, () -> contentService.delete(invalidId));
    assertThat(exception.getErrorCode()).isEqualTo(ContentErrorCode.CONTENT_NOT_FOUND);
  }

  @Test
  @DisplayName("존재하는 콘텐츠 ID로 단건 조회 시 상세 정보 리턴에 성공한다")
  void getContent_success_whenIdIsValid() {
    // given: 존재하는 콘텐츠 ID 및 조회 모킹 설정
    UUID contentId = UUID.randomUUID();
    Content content = Content.create("인셉션", ContentType.MOVIE, "SF 영화", "/uploads/test.jpg");
    when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));

    // when: 서비스 레이어 단건 조회 호출
    ContentDto result = contentService.find(contentId);

    // then: 상세 정보가 정상적으로 리턴되었는지 확인
    assertThat(result).isNotNull();
    assertThat(result.title()).isEqualTo("인셉션");
    assertThat(result.type()).isEqualTo("MOVIE");
  }

  @Test
  @DisplayName("존재하지 않는 콘텐츠 ID로 단건 조회 시 CONTENT_NOT_FOUND 예외가 발생한다")
  void getContent_fail_whenIdIsInvalid() {
    // given: 존재하지 않는 임의의 ID 구성
    UUID invalidId = UUID.randomUUID();
    when(contentRepository.findById(invalidId)).thenReturn(Optional.empty());

    // when & then: 단건 조회 호출 시 ContentException이 발생하는지 및 에러 코드가 CONTENT_NOT_FOUND인지 확인
    ContentException exception =
        Assertions.assertThrows(ContentException.class, () -> contentService.find(invalidId));
    assertThat(exception.getErrorCode()).isEqualTo(ContentErrorCode.CONTENT_NOT_FOUND);
  }

  @Test
  @DisplayName("목록 조회 요청 시 조건에 맞는 콘텐츠 슬라이스 데이터 리턴에 성공한다")
  void searchContents_success_whenDataIsValid() {
    // given: 검색 요청 DTO 구성 및 repository 조회 결과 모킹 설정
    ContentSearchRequest request =
        new ContentSearchRequest(
            ContentType.MOVIE,
            "인셉션",
            null,
            null,
            null,
            10,
            SortDirection.DESCENDING,
            ContentSortBy.CREATED_AT);
    Content content1 = Content.create("인셉션", ContentType.MOVIE, "SF 영화", "/uploads/test.jpg");
    Content content2 = Content.create("인셉션 2", ContentType.MOVIE, "SF 영화 2", "/uploads/test2.jpg");
    when(contentRepository.findAllByCondition(request)).thenReturn(List.of(content1, content2));
    when(contentRepository.countContents(request)).thenReturn(2L);

    // when: 서비스 레이어 목록 조회 호출
    CursorPageResponse<ContentDto> result = contentService.findAll(request);

    // then: 슬라이스 결과가 리턴되었는지 확인 (hasNext는 false여야 함, 가져온 개수가 limit보다 적기 때문)
    assertThat(result).isNotNull();
    assertThat(result.data()).hasSize(2);
    assertThat(result.hasNext()).isFalse();
    assertThat(result.nextCursor()).isNull();
    assertThat(result.totalCount()).isEqualTo(2L);
  }

  @Test
  @DisplayName("목록 조회 시 가져온 데이터 개수가 limit을 초과하면, 마지막 초과 데이터는 응답에서 제외되고 hasNext가 true로 반환된다")
  void searchContents_successWithSlicing_whenDataExceedsLimit() {
    // given: limit=2로 요청
    ContentSearchRequest request =
        new ContentSearchRequest(
            ContentType.MOVIE,
            null,
            null,
            null,
            null,
            2,
            SortDirection.DESCENDING,
            ContentSortBy.CREATED_AT);

    // Repository가 limit + 1개인 3개의 데이터를 반환하도록 모킹
    Content content1 = Content.create("콘텐츠1", ContentType.MOVIE, "설명1", "/uploads/1.jpg");
    Content content2 = Content.create("콘텐츠2", ContentType.MOVIE, "설명2", "/uploads/2.jpg");
    Content content3 = Content.create("콘텐츠3", ContentType.MOVIE, "설명3", "/uploads/3.jpg");

    ReflectionTestUtils.setField(content1, "id", UUID.randomUUID());
    ReflectionTestUtils.setField(content1, "createdAt", Instant.now());
    ReflectionTestUtils.setField(content2, "id", UUID.randomUUID());
    ReflectionTestUtils.setField(content2, "createdAt", Instant.now().plusSeconds(1));
    ReflectionTestUtils.setField(content3, "id", UUID.randomUUID());
    ReflectionTestUtils.setField(content3, "createdAt", Instant.now().plusSeconds(2));

    when(contentRepository.findAllByCondition(request))
        .thenReturn(List.of(content1, content2, content3));
    when(contentRepository.countContents(request)).thenReturn(3L);

    // when: 서비스 레이어 목록 조회 호출
    CursorPageResponse<ContentDto> result = contentService.findAll(request);

    // then: 응답 데이터 리스트는 limit(2)개여야 하고, hasNext는 true여야 함
    assertThat(result).isNotNull();
    assertThat(result.data()).hasSize(2);
    assertThat(result.data().get(0).title()).isEqualTo("콘텐츠1");
    assertThat(result.data().get(1).title()).isEqualTo("콘텐츠2");
    assertThat(result.hasNext()).isTrue();
    assertThat(result.nextIdAfter()).isEqualTo(content2.getId());
    assertThat(result.totalCount()).isEqualTo(3L);
  }
}
