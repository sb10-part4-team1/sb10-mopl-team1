package com.sb10.mopl.playlistsubscription.exception;

import com.sb10.mopl.common.exception.MoplException;
import java.util.Map;

public class PlaylistSubscriptionException extends MoplException {

  public PlaylistSubscriptionException(
      PlaylistSubscriptionErrorCode errorCode, Map<String, Object> details) {
    super(errorCode, details);
  }

  public PlaylistSubscriptionException(
      PlaylistSubscriptionErrorCode errorCode, Map<String, Object> details, Throwable cause) {
    super(errorCode, details, cause);
  }
}
