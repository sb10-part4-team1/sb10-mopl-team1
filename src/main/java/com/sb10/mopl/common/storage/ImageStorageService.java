package com.sb10.mopl.common.storage;

import org.springframework.web.multipart.MultipartFile;

public interface ImageStorageService {
  String upload(MultipartFile file);
}
