package com.sb10.mopl.conversation.repository;

import com.sb10.mopl.conversation.entity.Conversation;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationRepository
    extends JpaRepository<Conversation, UUID>, ConversationRepositoryCustom {

  // 두 유저가 이미 나눈 대화가 있는지 조회
  @Query(
      """
      SELECT c FROM Conversation c
      WHERE c.id IN (
          SELECT p.conversation.id FROM ConversationParticipant p WHERE p.user.id = :userId1
      )
      AND c.id IN (
          SELECT p.conversation.id FROM ConversationParticipant p WHERE p.user.id = :userId2
      )
      """)
  Optional<Conversation> findConversationByUserIds(
      @Param("userId1") UUID userId1, @Param("userId2") UUID userId2);
}
