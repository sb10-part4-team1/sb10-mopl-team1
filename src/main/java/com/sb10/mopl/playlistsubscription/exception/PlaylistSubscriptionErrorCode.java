package com.sb10.mopl.playlistsubscription.exception;

import com.sb10.mopl.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PlaylistSubscriptionErrorCode implements ErrorCode {
  PLAYLIST_SUBSCRIPTION_ALREADY_EXISTS(HttpStatus.CONFLICT, "PS02", "이미 구독한 플레이리스트입니다."),
  UNAUTHORIZED_PLAYLIST_SUBSCRIPTION_ACCESS(HttpStatus.FORBIDDEN, "PS03", "구독할 권한이 없습니다."),
  INVALID_PLAYLIST_SUBSCRIPTION_VALUE(HttpStatus.BAD_REQUEST, "PS04", "올바르지 않은 구독 값입니다.");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;
}
