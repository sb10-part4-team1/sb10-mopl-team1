package com.sb10.mopl.conversation.dto;

import java.util.UUID;

public record ConversationDto(
    UUID id, ConversationUserInfoDto with, DirectMessageDto lastestMessage, boolean hasUnread) {}
