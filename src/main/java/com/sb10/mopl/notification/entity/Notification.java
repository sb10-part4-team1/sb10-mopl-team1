package com.sb10.mopl.notification.entity;

import com.sb10.mopl.common.entity.BaseEntity;
import com.sb10.mopl.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "notifications",
    indexes = {@Index(name = "IDX_NOTIFICATIONS_USER_READ", columnList = "user_id, is_read")})
public class Notification extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "title", nullable = false, length = 255)
  private String title;

  @Column(name = "content", nullable = false, length = 255)
  private String content;

  @Enumerated(EnumType.STRING)
  @Column(name = "level", nullable = false, length = 20)
  private NotificationLevel level;

  @Column(name = "is_read", nullable = false)
  private boolean isRead;

  @Builder
  private Notification(User user, String title, String content, NotificationLevel level) {
    this.user = user;
    this.title = title;
    this.content = content;
    this.level = level;
    this.isRead = false;
  }

  public void updateIsRead(boolean isRead) {
    this.isRead = isRead;
  }
}
