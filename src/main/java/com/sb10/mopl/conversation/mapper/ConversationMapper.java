package com.sb10.mopl.conversation.mapper;

import com.sb10.mopl.conversation.dto.ConversationDto;
import com.sb10.mopl.conversation.dto.DirectMessageDto;
import com.sb10.mopl.conversation.entity.Conversation;
import com.sb10.mopl.conversation.entity.ConversationParticipant;
import com.sb10.mopl.conversation.entity.DirectMessage;
import com.sb10.mopl.user.mapper.UserMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = UserMapper.class)
public interface ConversationMapper {

  @Mapping(target = "id", source = "conversation.id")
  @Mapping(target = "with", source = "otherParticipant.user")
  @Mapping(target = "lastestMessage", source = "lastMessage")
  @Mapping(target = "hasUnread", source = "hasUnread")
  ConversationDto toDto(
    Conversation conversation,
    ConversationParticipant otherParticipant,
    DirectMessage lastMessage,
    boolean hasUnread
  );

  @Mapping(target = "conversationId", source = "directMessage.conversation.id")
  DirectMessageDto toDto(DirectMessage directMessage);
}
