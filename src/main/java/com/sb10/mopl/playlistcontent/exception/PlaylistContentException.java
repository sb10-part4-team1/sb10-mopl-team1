package com.sb10.mopl.playlistcontent.exception;

import com.sb10.mopl.common.exception.MoplException;
import java.util.Map;

public class PlaylistContentException extends MoplException {

  public PlaylistContentException(PlaylistContentErrorCode errorCode, Map<String, Object> details) {
    super(errorCode, details);
  }

  public PlaylistContentException(
      PlaylistContentErrorCode errorCode, Map<String, Object> details, Throwable cause) {
    super(errorCode, details, cause);
  }
}
