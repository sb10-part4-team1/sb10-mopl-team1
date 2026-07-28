package com.sb10.mopl.common.storage;

import com.sb10.mopl.common.storage.exception.StorageErrorCode;
import com.sb10.mopl.common.storage.exception.StorageException;
import io.awspring.cloud.s3.S3Template;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/*
 * AWS S3에 파일을 직접 업로드하는 운영 환경 전용 이미지 스토리지 서비스 구현체입니다.
 * Spring Cloud AWS 3.x의 S3Template을 활용하여 안전하게 클라우드 스토리징을 수행합니다.
 */
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
      s3Template.upload(bucket, s3Key, file.getInputStream());
      log.info("AWS S3 파일 업로드 성공 - 버킷: {}, 키: {}", bucket, s3Key);

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
    if (fileUrl == null || fileUrl.isBlank()) {
      return;
    }

    try {
      String s3Key;
      if (fileUrl.contains("/uploads/")) {
        s3Key = "uploads/" + fileUrl.substring(fileUrl.indexOf("/uploads/") + "/uploads/".length());
      } else {
        return;
      }

      s3Template.deleteObject(bucket, s3Key);
      log.info("AWS S3 파일 삭제 완료 - Bucket: {}, Key: {}", bucket, s3Key);
    } catch (Exception e) {
      log.error("AWS S3 파일 삭제 실패 - URL: {}", fileUrl, e);
    }
  }
}
