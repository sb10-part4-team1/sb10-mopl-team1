package com.sb10.mopl.common.storage;

import com.sb10.mopl.common.storage.exception.StorageErrorCode;
import com.sb10.mopl.common.storage.exception.StorageException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@Profile({"local", "default", "test"})
public class LocalStorageService implements ImageStorageService {

  private final String uploadDir;

  public LocalStorageService(@Value("${mopl.upload-dir}") String uploadDir) {
    this.uploadDir = uploadDir;
  }

  @Override
  public String upload(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      return null;
    }

    // 1. 이미지 파일 전용 유효성 검증
    ImageValidator.validate(file);

    try {
      Path uploadPath = Paths.get(uploadDir);
      if (!Files.exists(uploadPath)) {
        Files.createDirectories(uploadPath);
      }

      String originalFilename = file.getOriginalFilename();
      String extension = "";
      if (originalFilename != null && originalFilename.contains(".")) {
        extension = originalFilename.substring(originalFilename.lastIndexOf("."));
      }

      String savedFilename = UUID.randomUUID().toString() + extension;
      Path filePath = uploadPath.resolve(savedFilename);

      Files.copy(file.getInputStream(), filePath);

      return "/uploads/" + savedFilename;
    } catch (IOException e) {
      throw new StorageException(
          StorageErrorCode.FILE_UPLOAD_ERROR,
          Map.of("filename", String.valueOf(file.getOriginalFilename())),
          e);
    }
  }

  @Override
  public void delete(String fileUrl) {
    if (fileUrl == null || fileUrl.isBlank() || !fileUrl.startsWith("/uploads/")) {
      return;
    }

    try {
      String fileName = fileUrl.substring("/uploads/".length());
      Path filePath = Paths.get(uploadDir).resolve(fileName);
      Files.deleteIfExists(filePath);
      log.info("로컬 이미지 파일 삭제 완료 - Path: {}", filePath);
    } catch (IOException e) {
      log.error("로컬 이미지 파일 삭제 실패 - URL: {}", fileUrl, e);
    }
  }
}
