package com.sb10.mopl.notification.controller.api;

import com.sb10.mopl.auth.security.principal.AuthenticatedUser;
import com.sb10.mopl.common.exception.ErrorResponse;
import com.sb10.mopl.common.pagination.CursorPageResponse;
import com.sb10.mopl.notification.dto.NotificationDto;
import com.sb10.mopl.notification.dto.NotificationSearchRequest;
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

@Tag(name = "알림")
public interface NotificationControllerApiDocs {

  @Operation(summary = "알림 목록 조회 (커서 페이지네이션)", description = "API 요청자의 알림 목록만 조회할 수 있습니다.")
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
  ResponseEntity<CursorPageResponse<NotificationDto>> find(
      @Parameter(hidden = true) AuthenticatedUser currentUser,
      @ParameterObject NotificationSearchRequest request);

  @Operation(summary = "알림 읽음 처리")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "성공"),
    @ApiResponse(
        responseCode = "400",
        description = "잘못된 요청",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "401",
        description = "인증 오류",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "403",
        description = "권한 오류",
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
  ResponseEntity<Void> delete(
      @Parameter(description = "알림 ID") UUID notificationId,
      @Parameter(hidden = true) AuthenticatedUser currentUser);
}
