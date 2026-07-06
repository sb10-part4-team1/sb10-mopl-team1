package com.sb10.mopl.follow.entity;

import com.sb10.mopl.common.entity.BaseEntity;
import com.sb10.mopl.common.validation.DomainValidator;
import com.sb10.mopl.follow.exception.FollowErrorCode;
import com.sb10.mopl.follow.exception.FollowException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
    name = "follows",
    indexes = {
      @Index(name = "idx_follows_follower_id", columnList = "follower_id"),
      @Index(name = "idx_follows_followee_id", columnList = "followee_id")
    },
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_follows_follower_id_followee_id",
          columnNames = {"follower_id", "followee_id"})
    })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Follow extends BaseEntity {

  // 팔로우 하는 사용자 아이디
  @Column(name = "follower_id", nullable = false)
  private UUID followerId;

  // 팔로우 당하는 사용자 아이디
  @Column(name = "followee_id", nullable = false)
  private UUID followeeId;

  private static void validateCreate(UUID followerId, UUID followeeId) {
    DomainValidator.start()
        .check(followerId == null, "followerId", "팔로우 요청자는 필수입니다.")
        .check(followeeId == null, "followeeId", "팔로우 대상자는 필수입니다.")
        .check(
            followerId != null && followerId.equals(followeeId),
            "followeeId",
            "자기 자신은 팔로우할 수 없습니다.")
        .orThrow(details -> new FollowException(FollowErrorCode.INVALID_FOLLOW_VALUE, details));
  }

  public Follow(UUID followerId, UUID followeeId) {
    validateCreate(followerId, followeeId);
    this.followerId = followerId;
    this.followeeId = followeeId;
  }
}
