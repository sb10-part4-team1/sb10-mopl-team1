package com.sb10.mopl.follow.exception;

import com.sb10.mopl.common.exception.MoplException;
import java.util.Map;

public class FollowException extends MoplException {

  public FollowException(FollowErrorCode errorCode, Map<String, Object> details) {
    super(errorCode, details);
  }

  public FollowException(FollowErrorCode errorCode, Map<String, Object> details, Throwable cause) {
    super(errorCode, details, cause);
  }
}
