package com.sb10.mopl.common.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Profile({"local", "default", "test", "dev"})
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
      throw new RuntimeException("로컬 파일 업로드 중 오류가 발생했습니다.", e);
    }
  }
}
