package com.sb10.mopl.conversation.controller.api;

import com.sb10.mopl.auth.security.user.AuthenticatedUser;
import com.sb10.mopl.common.exception.ErrorResponse;
import com.sb10.mopl.common.pagination.CursorPageResponse;
import com.sb10.mopl.conversation.dto.ConversationCreateRequest;
import com.sb10.mopl.conversation.dto.ConversationDto;
import com.sb10.mopl.conversation.dto.ConversationSearchRequest;
import com.sb10.mopl.conversation.dto.DirectMessageDto;
import com.sb10.mopl.conversation.dto.DirectMessageSearchRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;

@Tag(name = "다이렉트 메시지")
public interface ConversationControllerApiDocs {

  @Operation(summary = "대화 생성")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "성공",
        content = @Content(schema = @Schema(implementation = ConversationDto.class))),
    @ApiResponse(
        responseCode = "400",
        description = "잘못된 요청",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "401",
        description = "인증 오류",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "404",
        description = "해당 리소스 없음",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "500",
        description = "서버 오류",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @SecurityRequirement(name = "BearerAuth")
  @SecurityRequirement(name = "CsrfToken")
  ResponseEntity<ConversationDto> createConversation(
      @Parameter(hidden = true) AuthenticatedUser currentUser, ConversationCreateRequest request);

  @Operation(summary = "대화 목록 조회 (커서 페이지네이션)", description = "API 요청자 본인의 대화 목록만 조회할 수 있습니다.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "성공"),
    @ApiResponse(
        responseCode = "400",
        description = "잘못된 요청",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "401",
        description = "인증 오류",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "500",
        description = "서버 오류",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @SecurityRequirement(name = "BearerAuth")
  ResponseEntity<CursorPageResponse<ConversationDto>> findConversations(
      @Parameter(hidden = true) AuthenticatedUser currentUser,
      @ParameterObject ConversationSearchRequest request);

  @Operation(summary = "특정 사용자와의 대화 조회")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "성공",
        content = @Content(schema = @Schema(implementation = ConversationDto.class))),
    @ApiResponse(
        responseCode = "400",
        description = "잘못된 요청",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "401",
        description = "인증 오류",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "404",
        description = "해당 리소스 없음",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "500",
        description = "서버 오류",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @SecurityRequirement(name = "BearerAuth")
  ResponseEntity<ConversationDto> findConversationWithUser(
      @Parameter(hidden = true) AuthenticatedUser currentUser,
      @Parameter(description = "대화 상대 사용자 ID") UUID userId);

  @Operation(summary = "대화 조회")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "성공",
        content = @Content(schema = @Schema(implementation = ConversationDto.class))),
    @ApiResponse(
        responseCode = "400",
        description = "잘못된 요청",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "401",
        description = "인증 오류",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "404",
        description = "해당 리소스 없음",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "500",
        description = "서버 오류",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @SecurityRequirement(name = "BearerAuth")
  ResponseEntity<ConversationDto> findConversation(
      @Parameter(hidden = true) AuthenticatedUser currentUser,
      @Parameter(description = "대화 ID") UUID conversationId);

  @Operation(
      summary = "DM 목록 조회 (커서 페이지네이션)",
      description = "특정 대화의 DM 목록을 조회합니다. API 요청자가 해당 대화의 참여자여야 합니다.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "성공"),
    @ApiResponse(
        responseCode = "400",
        description = "잘못된 요청",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "401",
        description = "인증 오류",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "404",
        description = "해당 리소스 없음",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "500",
        description = "서버 오류",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @SecurityRequirement(name = "BearerAuth")
  ResponseEntity<CursorPageResponse<DirectMessageDto>> findDirectMessages(
      @Parameter(hidden = true) AuthenticatedUser currentUser,
      @Parameter(description = "대화 ID") UUID conversationId,
      @ParameterObject DirectMessageSearchRequest request);

  @Operation(summary = "DM 읽음 처리")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "성공"),
    @ApiResponse(
        responseCode = "400",
        description = "잘못된 요청",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "401",
        description = "인증 오류",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "404",
        description = "해당 리소스 없음",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "500",
        description = "서버 오류",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  @SecurityRequirement(name = "BearerAuth")
  @SecurityRequirement(name = "CsrfToken")
  ResponseEntity<Void> readDirectMessage(
      @Parameter(hidden = true) AuthenticatedUser currentUser,
      @Parameter(description = "대화 ID") UUID conversationId,
      @Parameter(description = "DM ID") UUID directMessageId);
}
