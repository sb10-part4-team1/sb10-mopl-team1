package com.sb10.mopl.conversation.controller;

import com.sb10.mopl.auth.security.user.AuthenticatedUser;
import com.sb10.mopl.auth.security.user.CurrentUser;
import com.sb10.mopl.common.pagination.CursorPageResponse;
import com.sb10.mopl.conversation.dto.ConversationCreateRequest;
import com.sb10.mopl.conversation.dto.ConversationDto;
import com.sb10.mopl.conversation.dto.ConversationSearchRequest;
import com.sb10.mopl.conversation.dto.DirectMessageDto;
import com.sb10.mopl.conversation.dto.DirectMessageSearchRequest;
import com.sb10.mopl.conversation.service.ConversationService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

  private final ConversationService conversationService;

  // 대화 생성
  @PostMapping
  public ResponseEntity<ConversationDto> createConversation(
      @CurrentUser AuthenticatedUser currentUser,
      @Valid @RequestBody ConversationCreateRequest request) {
    ConversationDto dto = conversationService.createConversation(currentUser.id(), request);
    return ResponseEntity.ok(dto);
  }

  // 대화 목록 조회
  @GetMapping
  public ResponseEntity<CursorPageResponse<ConversationDto>> findConversations(
      @CurrentUser AuthenticatedUser currentUser,
      @ModelAttribute @Valid ConversationSearchRequest request) {
    CursorPageResponse<ConversationDto> response =
        conversationService.findConversations(currentUser.id(), request);
    return ResponseEntity.ok(response);
  }

  // 특정 사용자와의 대화 조회
  @GetMapping("/with")
  public ResponseEntity<ConversationDto> findConversationWithUser(
      @CurrentUser AuthenticatedUser currentUser, @RequestParam UUID userId) {
    ConversationDto dto = conversationService.findConversationWithUser(currentUser.id(), userId);
    return ResponseEntity.ok(dto);
  }

  // 특정 대화 조회
  @GetMapping("/{conversationId}")
  public ResponseEntity<ConversationDto> findConversation(
      @CurrentUser AuthenticatedUser currentUser, @PathVariable UUID conversationId) {
    ConversationDto dto = conversationService.findConversation(currentUser.id(), conversationId);
    return ResponseEntity.ok(dto);
  }

  // DM 목록 조회
  @GetMapping("/{conversationId}/direct-messages")
  public ResponseEntity<CursorPageResponse<DirectMessageDto>> findDirectMessages(
      @CurrentUser AuthenticatedUser currentUser,
      @PathVariable UUID conversationId,
      @ModelAttribute @Valid DirectMessageSearchRequest request) {
    CursorPageResponse<DirectMessageDto> response =
        conversationService.findDirectMessages(currentUser.id(), conversationId, request);
    return ResponseEntity.ok(response);
  }

  // DM 읽음 처리
  @PostMapping("/{conversationId}/direct-messages/{directMessageId}/read")
  public ResponseEntity<Void> readDirectMessage(
      @CurrentUser AuthenticatedUser currentUser,
      @PathVariable UUID conversationId,
      @PathVariable UUID directMessageId) {
    conversationService.readDirectMessage(currentUser.id(), conversationId, directMessageId);
    return ResponseEntity.ok().build();
  }
}
