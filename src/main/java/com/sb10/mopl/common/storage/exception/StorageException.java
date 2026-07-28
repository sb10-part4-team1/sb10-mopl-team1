package com.sb10.mopl.common.storage.exception;

import com.sb10.mopl.common.exception.MoplException;
import java.util.Collections;
import java.util.Map;

public class StorageException extends MoplException {

  public StorageException(StorageErrorCode errorCode) {
    super(errorCode, Collections.emptyMap());
  }

  public StorageException(StorageErrorCode errorCode, Throwable cause) {
    super(errorCode, Collections.emptyMap(), cause);
  }

  public StorageException(StorageErrorCode errorCode, Map<String, Object> details) {
    super(errorCode, details);
  }

  public StorageException(
      StorageErrorCode errorCode, Map<String, Object> details, Throwable cause) {
    super(errorCode, details, cause);
  }
}
