package com.sb10.mopl.conversation.entity;

import com.sb10.mopl.common.entity.BaseEntity;
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
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
  name = "conversation_participants",
  indexes = {@Index(name = "IDX_CONV_PARTICIPANTS_USER", columnList = "user_id")},
  uniqueConstraints = {
    @UniqueConstraint(
      name = "UQ_CONVERSATION_PARTICIPANTS_CONVERSATION_USER",
      columnNames = {"conversation_id", "user_id"})
  })
public class ConversationParticipant extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "conversation_id")
  private Conversation conversation;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user;

  public ConversationParticipant(Conversation conversation, User user) {
    this.conversation = conversation;
    this.user = user;
  }
}
