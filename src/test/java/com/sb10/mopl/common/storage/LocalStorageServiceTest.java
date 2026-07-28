package com.sb10.mopl.common.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sb10.mopl.common.storage.exception.StorageErrorCode;
import com.sb10.mopl.common.storage.exception.StorageException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class LocalStorageServiceTest {

  @TempDir Path tempDir;

  private LocalStorageService localStorageService;

  @BeforeEach
  void setUp() {
    localStorageService = new LocalStorageService(tempDir.toString());
  }

  @Nested
  @DisplayName("1. 로컬 파일 업로드 및 삭제 성공 케이스")
  class SuccessCases {

    @Test
    @DisplayName("확장자가 포함된 정상 이미지 파일 업로드 시 로컬 디렉토리에 저장되고 웹 접근 경로를 반환한다")
    void upload_success_whenFileWithExtensionIsValid() {
      // given: 확장자가 포함된 정상 이미지 파일 준비
      MockMultipartFile file =
          new MockMultipartFile("file", "test-image.png", "image/png", "sample content".getBytes());

      // when: 파일 업로드 수행
      String resultUrl = localStorageService.upload(file);

      // that: 업로드 결과 경로가 /uploads/로 시작하고 .png 확장자를 가지고 있으며 실제 파일이 디렉토리에 존재함을 검증한다
      assertThat(resultUrl).isNotNull().startsWith("/uploads/").endsWith(".png");

      String savedFileName = resultUrl.substring("/uploads/".length());
      Path savedFilePath = tempDir.resolve(savedFileName);
      assertThat(Files.exists(savedFilePath)).isTrue();
    }

    @Test
    @DisplayName("업로드된 로컬 파일 삭제(delete) 수행 시 실제 디렉터리에서 파일이 제거된다")
    void delete_success_whenFileExists() throws Exception {
      // given: 임시 디렉터리에 실제 파일 생성 후 URL 준비
      Path targetFile = tempDir.resolve("delete-target.png");
      Files.write(targetFile, "dummy bytes".getBytes());
      assertThat(Files.exists(targetFile)).isTrue();

      String fileUrl = "/uploads/delete-target.png";

      // when: delete 호출
      localStorageService.delete(fileUrl);

      // that: 디렉터리에서 파일이 성공적으로 제거되었는지 검증한다
      assertThat(Files.exists(targetFile)).isFalse();
    }

    @Test
    @DisplayName("null이거나 /uploads/ 로 시작하지 않는 유효하지 않은 URL 전달 시 예외 없이 안전하게 무시된다")
    void delete_success_whenUrlIsInvalidOrNull() {
      // when & that: null 및 유효하지 않은 URL 전달 시 안전 무시 검증한다
      assertThatCode(() -> localStorageService.delete(null)).doesNotThrowAnyException();
      assertThatCode(() -> localStorageService.delete("http://external.com/image.png"))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("상위 디렉터리 탈옥(Path Traversal) 공격 시도 URL 전달 시 파일 삭제 없이 안전하게 경고 후 차단된다")
    void delete_ignores_whenPathTraversalAttempted() throws Exception {
      // given: 임시 디렉터리 외부에 상위 보안 파일 생성
      Path secretFile = tempDir.resolve("secret.txt");
      Files.write(secretFile, "secret data".getBytes());
      assertThat(Files.exists(secretFile)).isTrue();

      // when: uploads 하위에서 상위로 이탈하는 Path Traversal URL 전달
      String attackUrl = "/uploads/../secret.txt";
      localStorageService.delete(attackUrl);

      // that: 상위 secret.txt 파일이 삭제되지 않고 안전하게 보존되었는지 검증한다
      assertThat(Files.exists(secretFile)).isTrue();
    }
  }

  @Nested
  @DisplayName("2. 로컬 파일 업로드 실패 및 엣지 케이스")
  class ExceptionCases {

    @Test
    @DisplayName("파일이 null이면 null을 반환하고 업로드를 수행하지 않는다")
    void upload_returnsNull_whenFileIsNull() {
      // when: null 파일로 업로드 수행
      String resultUrl = localStorageService.upload(null);

      // that: null 반환 검증
      assertThat(resultUrl).isNull();
    }

    @Test
    @DisplayName("파일이 0바이트로 비어있으면 null을 반환한다")
    void upload_returnsNull_whenFileIsEmpty() {
      // given: 0바이트 빈 파일 준비
      MockMultipartFile emptyFile =
          new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

      // when: 파일 업로드 수행
      String resultUrl = localStorageService.upload(emptyFile);

      // that: null 반환 검증
      assertThat(resultUrl).isNull();
    }

    @Test
    @DisplayName(
        "이미지 확장자나 MIME 타입이 아닌 일반 텍스트/문서 파일 전달 시 StorageException(INVALID_IMAGE_FILE) 예외가 발생한다")
    void upload_fail_whenFileIsNotAnImage() {
      // given: 이미지가 아닌 text/plain 문서 파일 준비
      MockMultipartFile documentFile =
          new MockMultipartFile("file", "document.txt", "text/plain", "text data".getBytes());

      // when & that: StorageException(ST01) 예외 발생 검증한다
      assertThatThrownBy(() -> localStorageService.upload(documentFile))
          .isInstanceOf(StorageException.class)
          .extracting("errorCode")
          .isEqualTo(StorageErrorCode.INVALID_IMAGE_FILE);
    }

    @Test
    @DisplayName("파일 스트림 읽기 중 IOException이 발생하면 StorageException(FILE_UPLOAD_ERROR) 예외가 발생한다")
    void upload_fail_whenIoExceptionOccurs() throws Exception {
      // given: getInputStream 호출 시 IOException이 터지는 목 MultipartFile 준비
      MultipartFile brokenFile = mock(MultipartFile.class);
      when(brokenFile.isEmpty()).thenReturn(false);
      when(brokenFile.getOriginalFilename()).thenReturn("broken.png");
      when(brokenFile.getContentType()).thenReturn("image/png");
      when(brokenFile.getInputStream()).thenThrow(new IOException("Stream error"));

      // when & that: StorageException(ST02) 발생 및 메시지 검증한다
      assertThatThrownBy(() -> localStorageService.upload(brokenFile))
          .isInstanceOf(StorageException.class)
          .extracting("errorCode")
          .isEqualTo(StorageErrorCode.FILE_UPLOAD_ERROR);
    }
  }
}
