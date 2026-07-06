package com.sb10.mopl.playlist.exception;

import com.sb10.mopl.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PlaylistErrorCode implements ErrorCode {
  PLAYLIST_NOT_FOUND(HttpStatus.NOT_FOUND, "PL01", "플레이리스트를 찾을 수 없습니다."),
  PLAYLIST_ALREADY_EXISTS(HttpStatus.CONFLICT, "PL02", "이미 플레이리스트를 생성하였습니다."),
  UNAUTHORIZED_PLAYLIST_ACCESS(HttpStatus.FORBIDDEN, "PL03", "플레이 리스트에 접근할 권한이 없습니다."),
  INVALID_PLAYLIST_VALUE(HttpStatus.BAD_REQUEST, "PL04", "올바르지 않은 플레이리스트 값입니다.");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;
}
