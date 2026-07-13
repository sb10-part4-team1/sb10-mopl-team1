package com.sb10.mopl.auth.entity;

import com.sb10.mopl.common.entity.BaseEntity;
import com.sb10.mopl.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "social_accounts",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "UQ_SOCIAL_ACCOUNTS_PROVIDER",
          columnNames = {"provider", "provider_user_id"})
    })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SocialAccount extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(name = "provider", nullable = false, length = 20)
  private SocialProvider provider;

  @Column(name = "provider_user_id", nullable = false, length = 255)
  private String providerUserId;

  private SocialAccount(User user, SocialProvider provider, String providerUserId) {
    this.user = user;
    this.provider = provider;
    this.providerUserId = providerUserId;
  }

  public static SocialAccount create(User user, SocialProvider provider, String providerUserId) {
    return new SocialAccount(user, provider, providerUserId);
  }
}
