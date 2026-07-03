package com.sb10.mopl.conversation.dto;

import java.time.Instant;
import java.util.UUID;

public record DirectMessageDto(
    UUID id,
    UUID conversationId,
    Instant createdAt,
    ConversationUserInfoDto sender,
    ConversationUserInfoDto receiver,
    String content) {}
