package com.sb10.mopl.common.storage;

import com.sb10.mopl.common.storage.exception.StorageErrorCode;
import com.sb10.mopl.common.storage.exception.StorageException;
import io.awspring.cloud.s3.S3Template;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/* AWS S3 전용 이미지 동기 스토리지 서비스 구현체입니다. */
@Slf4j
@Service
@Profile({"dev", "prod"})
@RequiredArgsConstructor
public class S3StorageService implements ImageStorageService {

  private final S3Template s3Template;

  @Value("${spring.cloud.aws.s3.bucket}")
  private String bucket;

  @Value("${spring.cloud.aws.s3.endpoint:}")
  private String endpoint;

  @Value("${spring.cloud.aws.region.static:ap-northeast-2}")
  private String region;

  @Override
  public String upload(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      return null;
    }

    // 1. 이미지 파일 전용 유효성 검증
    ImageValidator.validate(file);

    String originalFilename = file.getOriginalFilename();
    String extension = "";
    if (originalFilename != null && originalFilename.contains(".")) {
      extension = originalFilename.substring(originalFilename.lastIndexOf("."));
    }

    String savedFilename = UUID.randomUUID().toString() + extension;
    String s3Key = "uploads/" + savedFilename;

    try {
      // 2. S3 동기 업로드 수행
      s3Template.upload(bucket, s3Key, file.getInputStream());
      log.info("AWS S3 동기 파일 업로드 성공 - Bucket: {}, Key: {}", bucket, s3Key);

      // 3. 클라이언트 접근 URL 반환
      if (endpoint != null && !endpoint.isBlank()) {
        return endpoint + "/" + s3Key;
      }
      return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, s3Key);

    } catch (IOException e) {
      log.error("AWS S3 파일 업로드 실패", e);
      throw new StorageException(
          StorageErrorCode.FILE_UPLOAD_ERROR,
          Map.of("filename", String.valueOf(originalFilename)),
          e);
    }
  }

  @Override
  public void delete(String fileUrl) {
    if (fileUrl == null || fileUrl.isBlank() || !fileUrl.contains("/uploads/")) {
      return;
    }

    try {
      String s3Key =
          "uploads/" + fileUrl.substring(fileUrl.indexOf("/uploads/") + "/uploads/".length());
      String normalizedKey = Paths.get(s3Key).normalize().toString();

      // uploads/ 경로 이탈 시도 감지 차단
      if (!normalizedKey.startsWith("uploads/")) {
        log.warn("AWS S3 잘못된 삭제 경로 시도 감지 - URL: {}", fileUrl);
        return;
      }

      s3Template.deleteObject(bucket, normalizedKey);
      log.info("AWS S3 파일 삭제 완료 - Bucket: {}, Key: {}", bucket, normalizedKey);
    } catch (Exception e) {
      log.error("AWS S3 파일 삭제 실패 - URL: {}", fileUrl, e);
    }
  }
}
