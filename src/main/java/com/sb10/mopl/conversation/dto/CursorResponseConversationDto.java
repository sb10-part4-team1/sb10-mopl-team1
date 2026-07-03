package com.sb10.mopl.conversation.dto;

import java.util.List;
import java.util.UUID;

public record CursorResponseConversationDto(
  List<ConversationDto> data,
  String nextCursor,
  UUID nextIdAfter,
  boolean hasNext,
  Integer totalCount,
  String sortBy,
  String sortDirection
) {

}
