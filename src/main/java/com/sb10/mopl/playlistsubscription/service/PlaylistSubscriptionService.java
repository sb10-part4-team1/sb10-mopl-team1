package com.sb10.mopl.playlistsubscription.service;

import java.util.UUID;

public interface PlaylistSubscriptionService {
  void subscribe(UUID subscriberId, UUID playlistId);

  void unsubscribe(UUID subscriberId, UUID playlistId);
}
