package com.sb10.mopl.playlist.exception;

import com.sb10.mopl.common.exception.MoplException;
import java.util.Map;

public class PlaylistException extends MoplException {

  public PlaylistException(PlaylistErrorCode errorCode, Map<String, Object> details) {
    super(errorCode, details);
  }

  public PlaylistException(
      PlaylistErrorCode errorCode, Map<String, Object> details, Throwable cause) {
    super(errorCode, details, cause);
  }
}
