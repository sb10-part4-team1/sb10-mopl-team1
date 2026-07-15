package com.sb10.mopl.playlistsubscription.repository;

import com.sb10.mopl.playlistsubscription.entity.PlaylistSubscription;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlaylistSubscriptionRepository
    extends JpaRepository<PlaylistSubscription, UUID>, PlaylistSubscriptionRepositoryCustom {

  interface PlaylistSubscriptionCountProjection {

    UUID getPlaylistId();

    Long getSubscriberCount();
  }
}
