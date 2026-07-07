package com.sb10.mopl.conversation.controller;

import com.sb10.mopl.auth.security.user.MoplUserDetails;
import com.sb10.mopl.common.pagination.CursorPageResponse;
import com.sb10.mopl.conversation.dto.ConversationCreateRequest;
import com.sb10.mopl.conversation.dto.ConversationDto;
import com.sb10.mopl.conversation.dto.ConversationSearchRequest;
import com.sb10.mopl.conversation.service.ConversationService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
      @AuthenticationPrincipal MoplUserDetails userDetails,
      @Valid @RequestBody ConversationCreateRequest request) {
    ConversationDto dto = conversationService.createConversation(userDetails.getId(), request);
    return ResponseEntity.ok(dto);
  }

  // 대화 목록 조회
  @GetMapping
  public ResponseEntity<CursorPageResponse<ConversationDto>> findConversations(
      @AuthenticationPrincipal MoplUserDetails userDetails,
      @ModelAttribute @Valid ConversationSearchRequest request) {
    CursorPageResponse<ConversationDto> response =
        conversationService.findConversations(userDetails.getId(), request);
    return ResponseEntity.ok(response);
  }

  // 특정 사용자와의 대화 조회
  @GetMapping("/with")
  public ResponseEntity<ConversationDto> findConversationWithUser(
      @AuthenticationPrincipal MoplUserDetails userDetails, @RequestParam UUID userId) {
    ConversationDto dto = conversationService.findConversationWithUser(userDetails.getId(), userId);
    return ResponseEntity.ok(dto);
  }

  // 특정 대화 조회
  @GetMapping("/{conversationId}")
  public ResponseEntity<ConversationDto> findConversation(
      @AuthenticationPrincipal MoplUserDetails userDetails, @PathVariable UUID conversationId) {
    ConversationDto dto = conversationService.findConversation(userDetails.getId(), conversationId);
    return ResponseEntity.ok(dto);
  }
}
