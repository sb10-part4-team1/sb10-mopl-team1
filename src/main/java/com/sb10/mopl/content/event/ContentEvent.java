package com.sb10.mopl.content.event;

import java.util.UUID;

public record ContentEvent(UUID contentId, ContentEventType eventType) {

  public enum ContentEventType {
    CREATED,
    UPDATED,
    DELETED
  }
}
