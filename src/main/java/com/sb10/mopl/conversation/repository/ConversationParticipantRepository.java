package com.sb10.mopl.conversation.repository;

import com.sb10.mopl.conversation.entity.ConversationParticipant;
import com.sb10.mopl.conversation.entity.ConversationParticipantId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ConversationParticipantRepository
    extends JpaRepository<ConversationParticipant, ConversationParticipantId> {

  @Query(
      """
    SELECT p FROM ConversationParticipant p
    WHERE p.conversation.id = :conversationId AND p.user.id <> :myUserId
    """)
  Optional<ConversationParticipant> findOtherParticipant(UUID conversationId, UUID myUserId);

  @Query(
      """
    SELECT p FROM ConversationParticipant p
    JOIN FETCH p.user
    WHERE p.conversation.id IN :conversationIds AND p.user.id <> :myUserId
    """)
  List<ConversationParticipant> findOtherParticipants(List<UUID> conversationIds, UUID myUserId);
}
