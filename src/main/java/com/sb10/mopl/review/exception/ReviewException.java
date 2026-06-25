package com.sb10.mopl.review.exception;

import com.sb10.mopl.common.exception.MoplException;
import java.util.Map;

public class ReviewException extends MoplException {

  public ReviewException(ReviewErrorCode errorCode, Map<String, Object> details) {
    super(errorCode, details);
  }

  public ReviewException(
    ReviewErrorCode errorCode,
    Map<String, Object> details,
    Throwable cause
  ) {
    super(errorCode, details, cause);
  }
}
