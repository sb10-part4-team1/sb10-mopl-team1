package com.sb10.mopl.conversation.exception;

import com.sb10.mopl.common.exception.MoplException;
import java.util.Map;

public class ConversationException extends MoplException {

  public ConversationException(ConversationErrorCode errorCode, Map<String, Object> details) {
    super(errorCode, details);
  }

  public ConversationException(
      ConversationErrorCode errorCode, Map<String, Object> details, Throwable cause) {
    super(errorCode, details, cause);
  }
}
