package com.sb10.mopl.watchingsession.entity;

import com.sb10.mopl.common.entity.BaseEntity;
import com.sb10.mopl.content.entity.Content;
import com.sb10.mopl.user.entity.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
  name = "watching_session",
  indexes = {
    @Index(name = "IDX_WATCHING_SESSION_WATCHER", columnList = "watcher_id"),
    @Index(name = "IDX_WATCHING_SESSION_CONTENT", columnList = "content_id")
  },
  uniqueConstraints = {
    @UniqueConstraint(
        name = "UQ_WATCHING_SESSION_WATCHER_CONTENT",
        columnNames = {"watcher_id", "content_id"})
  })
public class WatchingSession extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "watcher_id", nullable = false)
  private User watcher;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "content_id", nullable = false)
  private Content content;

  @Builder
  private WatchingSession(User watcher, Content content) {
    this.watcher = watcher;
    this.content = content;
  }
}
