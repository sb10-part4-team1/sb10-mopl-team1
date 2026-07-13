package com.sb10.mopl.conversation.event;

import com.sb10.mopl.conversation.dto.DirectMessageDto;
import java.util.UUID;

public record DirectMessageSentEvent(UUID receiverId, DirectMessageDto directMessage) {}
