package com.sb10.mopl.conversation.repository;

import com.sb10.mopl.conversation.entity.DirectMessage;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DirectMessageRepository extends JpaRepository<DirectMessage, UUID> {

  Optional<DirectMessage> findTopByConversationIdOrderByCreatedAtDesc(UUID conversationId);

  boolean existsByConversationIdAndReceiverIdAndIsReadFalse(UUID conversationId, UUID receiverId);

  @Query(
      """
      SELECT dm FROM DirectMessage dm
      JOIN FETCH dm.sender
      JOIN FETCH dm.receiver
      WHERE dm.id IN (
        SELECT dm2.id FROM DirectMessage dm2
        WHERE dm2.conversation.id IN :conversationIds
        AND dm2.createdAt = (
          SELECT MAX(dm3.createdAt) FROM DirectMessage dm3
          WHERE dm3.conversation.id = dm2.conversation.id
        )
      )
      """)
  List<DirectMessage> findLastMessagesByConversationIds(List<UUID> conversationIds);

  @Query(
      """
      SELECT DISTINCT dm.conversation.id FROM DirectMessage dm
      WHERE dm.conversation.id IN :conversationIds
      AND dm.receiver.id = :receiverId
      AND dm.isRead = false
      """)
  List<UUID> findConversationIdsWithUnreadMessages(List<UUID> conversationIds, UUID receiverId);
}
