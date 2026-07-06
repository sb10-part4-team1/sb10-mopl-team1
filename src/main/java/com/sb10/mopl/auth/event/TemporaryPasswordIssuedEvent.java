package com.sb10.mopl.auth.event;

import java.util.UUID;

public record TemporaryPasswordIssuedEvent(
    UUID userId, UUID temporaryPasswordId, String email, String temporaryPassword) {

  @Override
  public String toString() {
    return "TemporaryPasswordIssuedEvent[userId="
        + userId
        + ", temporaryPasswordId="
        + temporaryPasswordId
        + "]";
  }
}
