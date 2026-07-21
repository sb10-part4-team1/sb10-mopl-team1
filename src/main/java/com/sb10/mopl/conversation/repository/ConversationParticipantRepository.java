package com.sb10.mopl.conversation.repository;

import com.sb10.mopl.conversation.entity.ConversationParticipant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationParticipantRepository
    extends JpaRepository<ConversationParticipant, UUID> {

  boolean existsByConversationIdAndUserId(UUID conversationId, UUID userId);

  @Query(
      """
      SELECT p FROM ConversationParticipant p
      WHERE p.conversation.id = :conversationId AND p.user.id <> :myUserId
      """)
  Optional<ConversationParticipant> findOtherParticipant(
      @Param("conversationId") UUID conversationId, @Param("myUserId") UUID myUserId);

  @Query(
      """
      SELECT p FROM ConversationParticipant p
      JOIN FETCH p.user
      WHERE p.conversation.id IN :conversationIds AND p.user.id <> :myUserId
      """)
  List<ConversationParticipant> findOtherParticipants(
      @Param("conversationIds") List<UUID> conversationIds, @Param("myUserId") UUID myUserId);
}
