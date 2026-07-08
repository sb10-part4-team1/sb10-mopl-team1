package com.sb10.mopl.conversation.exception;

import com.sb10.mopl.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public enum ConversationErrorCode implements ErrorCode {
  SELF_CONVERSATION_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "CV01", "자기 자신과 대화를 생성할 수 없습니다."),
  INVALID_CURSOR_VALUE(HttpStatus.BAD_REQUEST, "CV03", "올바르지 않은 커서 형식입니다."),
  CONVERSATION_NOT_FOUND(HttpStatus.NOT_FOUND, "CV04", "대화를 찾을 수 없습니다."),
  CONVERSATION_PARTICIPANT_NOT_FOUND(HttpStatus.NOT_FOUND, "CV05", "해당 사용자와의 대화를 찾을 수 없습니다.");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;
}
