package com.sb10.mopl.playlistcontent.exception;

import com.sb10.mopl.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PlaylistContentErrorCode implements ErrorCode {
  PLAYLIST_CONTENT_NOT_FOUND(HttpStatus.NOT_FOUND, "PC01", "플레이 리스트 콘텐츠를 찾을 수 없습니다."),
  PLAYLIST_CONTENT_ALREADY_EXISTS(HttpStatus.CONFLICT, "PC02", "이미 추가된 플레이리스트 콘텐츠입니다."),
  UNAUTHORIZED_PLAYLIST_CONTENT_ACCESS(HttpStatus.FORBIDDEN, "PC03", "플레이 리스트 콘텐츠에 접근할 권한이 없습니다."),
  INVALID_PLAYLIST_CONTENT_VALUE(HttpStatus.BAD_REQUEST, "PC04", "올바르지 않은 플레이리스트 콘텐츠 값입니다.");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;
}
