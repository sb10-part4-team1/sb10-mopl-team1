package com.sb10.mopl.conversation.service;

import com.sb10.mopl.common.pagination.CursorPageResponse;
import com.sb10.mopl.conversation.dto.ConversationCreateRequest;
import com.sb10.mopl.conversation.dto.ConversationDto;
import com.sb10.mopl.conversation.dto.ConversationSearchRequest;
import com.sb10.mopl.conversation.entity.Conversation;
import com.sb10.mopl.conversation.entity.ConversationParticipant;
import com.sb10.mopl.conversation.entity.ConversationParticipantId;
import com.sb10.mopl.conversation.entity.DirectMessage;
import com.sb10.mopl.conversation.exception.ConversationErrorCode;
import com.sb10.mopl.conversation.exception.ConversationException;
import com.sb10.mopl.conversation.mapper.ConversationMapper;
import com.sb10.mopl.conversation.repository.ConversationParticipantRepository;
import com.sb10.mopl.conversation.repository.ConversationRepository;
import com.sb10.mopl.conversation.repository.DirectMessageRepository;
import com.sb10.mopl.user.entity.User;
import com.sb10.mopl.user.exception.UserErrorCode;
import com.sb10.mopl.user.exception.UserException;
import com.sb10.mopl.user.repository.UserRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ConversationService {

  private final ConversationRepository conversationRepository;
  private final ConversationParticipantRepository conversationParticipantRepository;
  private final DirectMessageRepository directMessageRepository;
  private final UserRepository userRepository;

  private final ConversationMapper conversationMapper;

  // 대화 생성
  public ConversationDto createConversation(UUID requestUserId, ConversationCreateRequest request) {
    UUID withUserId = request.withUserId();

    if (requestUserId.equals(withUserId)) {
      throw new ConversationException(
          ConversationErrorCode.SELF_CONVERSATION_NOT_ALLOWED, Map.of("id", withUserId));
    }

    // 이미 존재하는 대화가 있으면 그대로 반환 (중복 생성 방지)
    Optional<Conversation> existing =
        conversationRepository.findConversationByUserIds(requestUserId, withUserId);
    if (existing.isPresent()) {
      return toDto(existing.get(), requestUserId);
    }

    User me =
        userRepository
            .findById(requestUserId)
            .orElseThrow(
                () ->
                    new UserException(
                        UserErrorCode.USER_NOT_FOUND, Map.of("userId", requestUserId)));
    User withUser =
        userRepository
            .findById(withUserId)
            .orElseThrow(
                () ->
                    new UserException(UserErrorCode.USER_NOT_FOUND, Map.of("userId", withUserId)));

    Conversation conversation = new Conversation();
    conversationRepository.save(conversation);

    conversationParticipantRepository.save(new ConversationParticipant(conversation, me));
    conversationParticipantRepository.save(new ConversationParticipant(conversation, withUser));

    return toDto(conversation, requestUserId);
  }

  // 대화 조회
  @Transactional(readOnly = true)
  public CursorPageResponse<ConversationDto> findConversations(
      UUID myUserId, ConversationSearchRequest request) {
    List<Conversation> result = conversationRepository.search(myUserId, request);

    boolean hasNext = result.size() > request.limit();
    List<Conversation> data = hasNext ? result.subList(0, request.limit()) : result;

    List<ConversationDto> dtos = toDtos(data, myUserId);

    String nextCursor = null;
    UUID nextIdAfter = null;
    if (hasNext && !data.isEmpty()) {
      Conversation last = data.get(data.size() - 1);
      nextCursor = last.getCreatedAt().toString();
      nextIdAfter = last.getId();
    }

    long totalCount = conversationRepository.countConversations(myUserId, request);

    return new CursorPageResponse<>(
        dtos,
        nextCursor,
        nextIdAfter,
        hasNext,
        totalCount,
        request.sortBy(),
        request.sortDirection());
  }

  // 특정 대화 조회
  @Transactional(readOnly = true)
  public ConversationDto findConversation(UUID myUserId, UUID conversationId) {
    Conversation conversation =
        conversationRepository
            .findById(conversationId)
            .orElseThrow(
                () ->
                    new ConversationException(
                        ConversationErrorCode.CONVERSATION_NOT_FOUND,
                        Map.of("conversationId", conversationId)));

    boolean isParticipant =
        conversationParticipantRepository.existsById(
            new ConversationParticipantId(conversationId, myUserId));
    if (!isParticipant) {
      // 참여자가 아니면 존재 여부 노출 방지를 위해 404로 통일
      throw new ConversationException(
          ConversationErrorCode.CONVERSATION_NOT_FOUND, Map.of("conversationId", conversationId));
    }

    return toDto(conversation, myUserId);
  }

  // 특정 사용자와의 대화 조회
  @Transactional(readOnly = true)
  public ConversationDto findConversationWithUser(UUID myUserId, UUID withUserId) {
    Conversation conversation =
        conversationRepository
            .findConversationByUserIds(myUserId, withUserId)
            .orElseThrow(
                () ->
                    new ConversationException(
                        ConversationErrorCode.CONVERSATION_PARTICIPANT_NOT_FOUND,
                        Map.of("withUserId", withUserId)));

    return toDto(conversation, myUserId);
  }

  private ConversationDto toDto(Conversation conversation, UUID myUserId) {
    return toDtos(List.of(conversation), myUserId).get(0);
  }

  /**
   * 여러 대화를 한 번에 DTO로 변환합니다.
   *
   * <p>대화별로 상대방/마지막 메시지/안읽음 여부를 개별 조회하면 N+1 쿼리가 발생하므로, 전체 대화 ID를 모아 배치로 조회한 뒤 메모리에서 조립합니다.
   */
  private List<ConversationDto> toDtos(List<Conversation> conversations, UUID myUserId) {
    if (conversations.isEmpty()) {
      return List.of();
    }

    List<UUID> conversationIds = conversations.stream().map(Conversation::getId).toList();

    Map<UUID, ConversationParticipant> otherParticipantsByConversationId =
        conversationParticipantRepository.findOtherParticipants(conversationIds, myUserId).stream()
            .collect(Collectors.toMap(p -> p.getConversation().getId(), Function.identity()));

    Map<UUID, DirectMessage> lastMessagesByConversationId =
        directMessageRepository.findLastMessagesByConversationIds(conversationIds).stream()
            .collect(
                Collectors.toMap(
                    dm -> dm.getConversation().getId(),
                    Function.identity(),
                    (first, second) -> first));

    Set<UUID> conversationIdsWithUnread =
        new HashSet<>(
            directMessageRepository.findConversationIdsWithUnreadMessages(
                conversationIds, myUserId));

    return conversations.stream()
        .map(
            conversation -> {
              ConversationParticipant other =
                  otherParticipantsByConversationId.get(conversation.getId());
              if (other == null) {
                throw new ConversationException(
                    ConversationErrorCode.CONVERSATION_NOT_FOUND,
                    Map.of("conversationId", conversation.getId()));
              }
              DirectMessage lastMessage = lastMessagesByConversationId.get(conversation.getId());
              boolean hasUnread = conversationIdsWithUnread.contains(conversation.getId());
              return conversationMapper.toDto(conversation, other, lastMessage, hasUnread);
            })
        .toList();
  }
}
