package com.sb10.mopl.playlistsubscription.entity;

import com.sb10.mopl.common.entity.BaseEntity;
import com.sb10.mopl.common.validation.DomainValidator;
import com.sb10.mopl.playlist.entity.Playlist;
import com.sb10.mopl.playlistsubscription.exception.PlaylistSubscriptionErrorCode;
import com.sb10.mopl.playlistsubscription.exception.PlaylistSubscriptionException;
import com.sb10.mopl.user.entity.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(
    name = "playlist_subscriptions",
    indexes = {
      @Index(name = "IDX_PLAYLIST_SUBSCRIPTIONS_SUBSCRIBER_ID", columnList = "subscriber_id")
    },
    uniqueConstraints = {
      @UniqueConstraint(
          name = "UQ_PLAYLIST_SUBSCRIPTIONS",
          columnNames = {"playlist_id", "subscriber_id"})
    })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaylistSubscription extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "subscriber_id", nullable = false)
  private User subscriber;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "playlist_id", nullable = false)
  private Playlist playlist;

  public PlaylistSubscription(User subscriber, Playlist playlist) {
    validateCreate(subscriber, playlist);
    this.subscriber = subscriber;
    this.playlist = playlist;
  }

  private static void validateCreate(User subscriber, Playlist playlist) {
    DomainValidator.start()
        .check(subscriber == null, "subscriber", "구독 요청자는 필수입니다.")
        .check(playlist == null, "playlist", "플레이리스트는 필수입니다.")
        .orThrow(
            details ->
                new PlaylistSubscriptionException(
                    PlaylistSubscriptionErrorCode.INVALID_PLAYLIST_SUBSCRIPTION_VALUE, details));
  }
}
