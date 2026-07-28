package com.sb10.mopl.common.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sb10.mopl.common.storage.exception.StorageErrorCode;
import com.sb10.mopl.common.storage.exception.StorageException;
import io.awspring.cloud.s3.S3Template;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class S3StorageServiceTest {

  @Mock private S3Template s3Template;

  private S3StorageService s3StorageService;

  @BeforeEach
  void setUp() {
    s3StorageService = new S3StorageService(s3Template);
    ReflectionTestUtils.setField(s3StorageService, "bucket", "my-test-bucket");
    ReflectionTestUtils.setField(s3StorageService, "region", "ap-northeast-2");
  }

  @Nested
  @DisplayName("1. S3 동기 파일 업로드 및 삭제 성공 케이스")
  class SuccessCases {

    @Test
    @DisplayName("Endpoint가 설정되어 있을 때 S3Template으로 업로드하고 Custom Endpoint URL 포맷을 반환한다")
    void upload_success_whenCustomEndpointIsProvided() {
      // given: custom endpoint 주입 및 정상 멀티파트 파일 준비
      ReflectionTestUtils.setField(s3StorageService, "endpoint", "https://cdn.mopl.com");
      MockMultipartFile file =
          new MockMultipartFile("file", "poster.png", "image/png", "poster bytes".getBytes());

      // when: S3 파일 업로드 호출
      String resultUrl = s3StorageService.upload(file);

      // that: custom endpoint 주소 기반 S3 Key URL이 반환되고 S3Template.upload가 수행되었는지 검증한다
      assertThat(resultUrl)
          .isNotNull()
          .startsWith("https://cdn.mopl.com/uploads/")
          .endsWith(".png");
      verify(s3Template).upload(eq("my-test-bucket"), any(String.class), any(InputStream.class));
    }

    @Test
    @DisplayName("Endpoint가 설정되지 않은 경우 표준 AWS S3 버킷 URL 포맷을 반환한다")
    void upload_success_whenEndpointIsNull() {
      // given: endpoint를 null로 설정 및 정상 이미지 파일 준비
      ReflectionTestUtils.setField(s3StorageService, "endpoint", null);
      MockMultipartFile file =
          new MockMultipartFile("file", "banner.jpg", "image/jpeg", "banner bytes".getBytes());

      // when: S3 파일 업로드 호출
      String resultUrl = s3StorageService.upload(file);

      // that: 표준 AWS S3 URL 포맷 반환 및 업로드 실행을 검증한다
      assertThat(resultUrl)
          .isNotNull()
          .startsWith("https://my-test-bucket.s3.ap-northeast-2.amazonaws.com/uploads/")
          .endsWith(".jpg");
      verify(s3Template).upload(eq("my-test-bucket"), any(String.class), any(InputStream.class));
    }

    @Test
    @DisplayName("S3 이미지 파일 삭제(delete) 호출 시 S3Template.deleteObject를 수행한다")
    void delete_success_whenValidFileUrlIsGiven() {
      // given: 삭제 대상 S3 URL 준비
      String fileUrl = "https://cdn.mopl.com/uploads/test-image.jpg";

      // when: 삭제 수행
      s3StorageService.delete(fileUrl);

      // that: S3Template.deleteObject 호출되었는지 검증한다
      verify(s3Template).deleteObject("my-test-bucket", "uploads/test-image.jpg");
    }
  }

  @Nested
  @DisplayName("2. S3 파일 업로드 실패 및 엣지 케이스")
  class ExceptionCases {

    @Test
    @DisplayName("파일이 null이면 null을 반환한다")
    void upload_returnsNull_whenFileIsNull() {
      // when: null 파일 전달
      String resultUrl = s3StorageService.upload(null);

      // that: null 반환 검증
      assertThat(resultUrl).isNull();
    }

    @Test
    @DisplayName("이미지 확장자나 MIME 타입이 아닌 파일 제출 시 StorageException(INVALID_IMAGE_FILE) 예외가 발생한다")
    void upload_fail_whenFileIsNotAnImage() {
      // given: 이미지가 아닌 pdf 문서 파일 준비
      MockMultipartFile pdfFile =
          new MockMultipartFile("file", "document.pdf", "application/pdf", "pdf bytes".getBytes());

      // when & that: StorageException(ST01) 발생 검증한다
      assertThatThrownBy(() -> s3StorageService.upload(pdfFile))
          .isInstanceOf(StorageException.class)
          .extracting("errorCode")
          .isEqualTo(StorageErrorCode.INVALID_IMAGE_FILE);
    }

    @Test
    @DisplayName("파일 스트림 읽기 중 IOException 발생 시 StorageException(FILE_UPLOAD_ERROR) 예외가 발생한다")
    void upload_fail_whenIoExceptionOccurs() throws Exception {
      // given: getInputStream 호출 시 IOException이 터지는 목 파일 준비
      MultipartFile brokenFile = mock(MultipartFile.class);
      when(brokenFile.isEmpty()).thenReturn(false);
      when(brokenFile.getOriginalFilename()).thenReturn("broken.png");
      when(brokenFile.getContentType()).thenReturn("image/png");
      when(brokenFile.getInputStream()).thenThrow(new IOException("Read error"));

      // when & that: StorageException(ST02) 발생 검증한다
      assertThatThrownBy(() -> s3StorageService.upload(brokenFile))
          .isInstanceOf(StorageException.class)
          .extracting("errorCode")
          .isEqualTo(StorageErrorCode.FILE_UPLOAD_ERROR);
    }
  }
}
