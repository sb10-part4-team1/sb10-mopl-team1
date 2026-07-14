package com.sb10.mopl.common.storage;

import io.awspring.cloud.s3.S3Template;
import java.io.IOException;
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
@Profile({"prod", "aws"})
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
      log.error("AWS S3 파일 업로드 작업 중 예외 발생", e);
      throw new RuntimeException("S3 파일 업로드 중 오류가 발생했습니다.", e);
    }
  }
}
