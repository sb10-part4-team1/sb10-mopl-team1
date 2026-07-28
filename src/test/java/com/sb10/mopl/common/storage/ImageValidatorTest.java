package com.sb10.mopl.common.storage;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sb10.mopl.common.storage.exception.StorageErrorCode;
import com.sb10.mopl.common.storage.exception.StorageException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class ImageValidatorTest {

  @Nested
  @DisplayName("1. 이미지 파일 검증 성공 케이스")
  class SuccessCases {

    @Test
    @DisplayName("정상적인 PNG 파일은 검증을 통과한다")
    void validate_success_whenPngImageIsValid() {
      // given: 정상 PNG 이미지 파일 준비
      MockMultipartFile file =
          new MockMultipartFile("file", "image.png", "image/png", "png data".getBytes());

      // when & that: 예외 없이 통과하는지 검증한다
      assertThatCode(() -> ImageValidator.validate(file)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("확장자에 대문자가 포함되어 있어도 정상 검증을 통과한다")
    void validate_success_whenExtensionIsUpperCase() {
      // given: 대문자 확장자(.JPG) 이미지 준비
      MockMultipartFile file =
          new MockMultipartFile("file", "PHOTO.JPG", "image/jpeg", "jpg data".getBytes());

      // when & that: 예외 없이 통과 검증한다
      assertThatCode(() -> ImageValidator.validate(file)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("파일명에 점(.)이 여러 개 들어있어도 마지막 확장자를 인식하여 검증을 통과한다")
    void validate_success_whenFileNameHasMultipleDots() {
      // given: 점이 다수 포함된 파일명 준비
      MockMultipartFile file =
          new MockMultipartFile(
              "file", "my.avatar.v1.final.webp", "image/webp", "webp data".getBytes());

      // when & that: 예외 없이 통과 검증한다
      assertThatCode(() -> ImageValidator.validate(file)).doesNotThrowAnyException();
    }
  }

  @Nested
  @DisplayName("2. 이미지 파일 검증 실패 케이스")
  class ExceptionCases {

    @Test
    @DisplayName("ContentType이 image/로 시작하지 않으면 StorageException(INVALID_IMAGE_FILE) 예외가 발생한다")
    void validate_fail_whenContentTypeIsNotImage() {
      // given: text/plain 멀티파트 파일 준비
      MockMultipartFile file =
          new MockMultipartFile("file", "test.png", "text/plain", "plain text".getBytes());

      // when & that: StorageException(ST01) 예외 발생 검증한다
      assertThatThrownBy(() -> ImageValidator.validate(file))
          .isInstanceOf(StorageException.class)
          .extracting("errorCode")
          .isEqualTo(StorageErrorCode.INVALID_IMAGE_FILE);
    }

    @Test
    @DisplayName("ContentType은 image/png 이지만 확장자가 .pdf 인 불일치 파일은 예외가 발생한다")
    void validate_fail_whenExtensionIsNotAllowed() {
      // given: MIME 타입과 확장자가 불일치하는 파일 준비
      MockMultipartFile file =
          new MockMultipartFile("file", "malicious.pdf", "image/png", "pdf data".getBytes());

      // when & that: StorageException(ST01) 예외 발생 검증한다
      assertThatThrownBy(() -> ImageValidator.validate(file))
          .isInstanceOf(StorageException.class)
          .extracting("errorCode")
          .isEqualTo(StorageErrorCode.INVALID_IMAGE_FILE);
    }
  }
}
