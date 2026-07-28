package com.sb10.mopl.common.storage;

import com.sb10.mopl.common.storage.exception.StorageErrorCode;
import com.sb10.mopl.common.storage.exception.StorageException;
import java.util.List;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;

/* 이미지 전용 파일 검증 유틸리티 클래스입니다. */
public final class ImageValidator {

  private static final List<String> ALLOWED_EXTENSIONS =
      List.of(".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp", ".svg");

  private ImageValidator() {
    // 인스턴스화 방지
  }

  /*
   * 멀티파트 파일이 유효한 이미지 파일인지 검증합니다.
   * 이미지 파일이 아니거나 null/empty인 경우 StorageException(INVALID_IMAGE_FILE)을 발생시킵니다.
   */
  public static void validate(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      return;
    }

    String contentType = file.getContentType();
    if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
      throw new StorageException(
          StorageErrorCode.INVALID_IMAGE_FILE,
          Map.of(
              "filename", String.valueOf(file.getOriginalFilename()),
              "contentType", String.valueOf(contentType)));
    }

    String originalFilename = file.getOriginalFilename();
    if (originalFilename != null && originalFilename.contains(".")) {
      String extension =
          originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
      if (!ALLOWED_EXTENSIONS.contains(extension)) {
        throw new StorageException(
            StorageErrorCode.INVALID_IMAGE_FILE,
            Map.of("filename", originalFilename, "extension", extension));
      }
    }
  }
}
