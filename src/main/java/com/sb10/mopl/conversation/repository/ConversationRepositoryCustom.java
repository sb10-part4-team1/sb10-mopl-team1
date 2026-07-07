package com.sb10.mopl.conversation.repository;

import com.sb10.mopl.conversation.dto.ConversationSearchRequest;
import com.sb10.mopl.conversation.entity.Conversation;
import java.util.List;
import java.util.UUID;

public interface ConversationRepositoryCustom {

  List<Conversation> search(UUID myUserId, ConversationSearchRequest request);

  long countConversations(UUID myUserId, ConversationSearchRequest request);

}
