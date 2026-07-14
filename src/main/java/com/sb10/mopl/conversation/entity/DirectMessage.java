package com.sb10.mopl.conversation.entity;

import com.sb10.mopl.common.entity.BaseEntity;
import com.sb10.mopl.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
    name = "direct_messages",
    indexes = {
      @Index(name = "IDX_DM_CONVERSATION_TIME", columnList = "conversation_id, created_at")
    })
public class DirectMessage extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "conversation_id", nullable = false)
  private Conversation conversation;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "sender_id", nullable = false)
  private User sender;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "receiver_id", nullable = false)
  private User receiver;

  @Column(name = "content", nullable = false, columnDefinition = "TEXT")
  private String content;

  @Column(name = "is_read", nullable = false)
  private boolean isRead;

  @Builder
  private DirectMessage(Conversation conversation, User sender, User receiver, String content) {
    this.conversation = conversation;
    this.sender = sender;
    this.receiver = receiver;
    this.content = content;
    this.isRead = false;
  }

  public void updateIsRead(boolean isRead) {
    this.isRead = isRead;
  }
}
