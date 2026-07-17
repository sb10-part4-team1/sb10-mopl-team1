package com.sb10.mopl.watchingsession.exception;

import com.sb10.mopl.common.exception.MoplException;
import java.util.Map;

public class WatchingSessionException extends MoplException {

  public WatchingSessionException(WatchingSessionErrorCode errorCode, Map<String, Object> details) {
    super(errorCode, details);
  }

  public WatchingSessionException(
      WatchingSessionErrorCode errorCode, Map<String, Object> details, Throwable cause) {
    super(errorCode, details, cause);
  }
}
