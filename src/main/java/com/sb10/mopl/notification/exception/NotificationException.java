package com.sb10.mopl.notification.exception;

import com.sb10.mopl.common.exception.MoplException;
import java.util.Map;

public class NotificationException extends MoplException {

  public NotificationException(NotificationErrorCode errorCode, Map<String, Object> details) {
    super(errorCode, details);
  }

  public NotificationException(
      NotificationErrorCode errorCode, Map<String, Object> details, Throwable cause) {
    super(errorCode, details, cause);
  }
}
