package com.sb10.mopl.playlistsubscription.entity;

import com.sb10.mopl.common.entity.BaseEntity;
import com.sb10.mopl.common.validation.DomainValidator;
import com.sb10.mopl.playlistsubscription.exception.PlaylistSubscriptionErrorCode;
import com.sb10.mopl.playlistsubscription.exception.PlaylistSubscriptionException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
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
  @Column(name = "subscriber_id", nullable = false)
  private UUID subscriberId;

  @Column(name = "playlist_id", nullable = false)
  private UUID playlistId;

  public PlaylistSubscription(UUID subscriberId, UUID playlistId) {
    validateCreate(subscriberId, playlistId);
    this.subscriberId = subscriberId;
    this.playlistId = playlistId;
  }

  private static void validateCreate(UUID subscriberId, UUID playlistId) {
    DomainValidator.start()
        .check(subscriberId == null, "subscriberId", "구독 요청자는 필수입니다.")
        .check(playlistId == null, "playlistId", "플레이 리스트는 필수입니다.")
        .orThrow(
            details ->
                new PlaylistSubscriptionException(
                    PlaylistSubscriptionErrorCode.INVALID_PLAYLIST_SUBSCRIPTION_VALUE, details));
  }
}
