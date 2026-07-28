package com.sb10.mopl.common.storage.exception;

import com.sb10.mopl.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public enum StorageErrorCode implements ErrorCode {
  INVALID_IMAGE_FILE(HttpStatus.BAD_REQUEST, "ST01", "이미지 파일만 업로드할 수 있습니다."),
  FILE_UPLOAD_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "ST02", "파일 업로드 작업 중 오류가 발생했습니다."),
  INVALID_FILE_NAME(HttpStatus.BAD_REQUEST, "ST03", "올바르지 않은 파일명입니다.");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;
}
