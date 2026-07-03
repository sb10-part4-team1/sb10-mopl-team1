package com.sb10.mopl.conversation.dto;

import java.util.UUID;

public record ConversationUserInfoDto(UUID userId, String name, String profileImageUrl) {}
