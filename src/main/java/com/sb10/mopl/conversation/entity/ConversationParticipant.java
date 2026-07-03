package com.sb10.mopl.conversation.entity;

import com.sb10.mopl.user.entity.User;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
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
    uniqueConstraints = {
      @UniqueConstraint(
          name = "IDX_CONV_PARTICIPANTS_USER",
          columnNames = {"user_id"})
    })

// @EqualsAndHashCode
public class ConversationParticipant {

  @EmbeddedId private ConversationParticipantId id;

  @MapsId("id") // todo: 필수?
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "conversation_id")
  private Conversation conversation;

  @MapsId("id")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user;

  // todo: joinedAt이 필요할까?

  public ConversationParticipant(Conversation conversation, User user) {
    this.conversation = conversation;
    this.user = user;
    this.id = new ConversationParticipantId(conversation.getId(), user.getId());
  }
}
