package com.sb10.mopl.conversation.repository;

import com.sb10.mopl.conversation.entity.Conversation;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ConversationRepository
    extends JpaRepository<Conversation, UUID>, ConversationRepositoryCustom {

  // 두 유저가 이미 나눈 대화가 있는지 조회
  @Query(
      """
      SELECT c FROM Conversation c
      WHERE c.id IN (
          SELECT p.id.conversationId FROM ConversationParticipant p WHERE p.id.userId = :userId1
      )
      AND c.id IN (
          SELECT p.id.conversationId FROM ConversationParticipant p WHERE p.id.userId = :userId2
      )
      """)
  Optional<Conversation> findConversationByUserIds(UUID userId1, UUID userId2);
}
