package com.sb10.mopl.playlistsubscription.repository;

import com.sb10.mopl.playlistsubscription.entity.PlaylistSubscription;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface PlaylistSubscriptionRepositoryCustom {

  boolean existsBySubscriberIdAndPlaylistId(UUID subscriberId, UUID playlistId);

  Optional<PlaylistSubscription> findBySubscriberIdAndPlaylistId(
      UUID subscriberId, UUID playlistId);

  long countByPlaylistId(UUID playlistId);

  List<PlaylistSubscriptionRepository.PlaylistSubscriptionCountProjection> countByPlaylistIds(
      Collection<UUID> playlistIds);

  Set<UUID> findSubscribedPlaylistIds(UUID subscriberId, Collection<UUID> playlistIds);

  List<UUID> findSubscriberIdsByPlaylistId(UUID playlistId);
}
