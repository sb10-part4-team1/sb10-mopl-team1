package com.sb10.mopl.conversation.entity;

import com.sb10.mopl.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(
    name = "conversation_participants",
    indexes = {@Index(name = "IDX_CONV_PARTICIPANTS_USER", columnList = "user_id")})
public class ConversationParticipant {

  @EmbeddedId private ConversationParticipantId id;

  @MapsId("conversationId")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "conversation_id")
  private Conversation conversation;

  @MapsId("userId")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  public ConversationParticipant(Conversation conversation, User user) {
    this.conversation = conversation;
    this.user = user;
    this.id = new ConversationParticipantId(conversation.getId(), user.getId());
  }
}
