package com.sb10.mopl.conversation.repository;

import com.sb10.mopl.conversation.dto.DirectMessageSearchRequest;
import com.sb10.mopl.conversation.entity.DirectMessage;
import java.util.List;
import java.util.UUID;

public interface DirectMessageRepositoryCustom {

  List<DirectMessage> search(UUID conversationId, DirectMessageSearchRequest request);

  long countMessages(UUID conversationId);
}
