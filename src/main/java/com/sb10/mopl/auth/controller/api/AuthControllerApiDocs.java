package com.sb10.mopl.auth.controller.api;

import com.sb10.mopl.auth.dto.request.ResetPasswordRequest;
import com.sb10.mopl.auth.dto.response.JwtDto;
import com.sb10.mopl.common.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;

@Tag(name = "인증 관리")
public interface AuthControllerApiDocs {

  @Operation(
      summary = "토큰 재발급",
      description = "쿠키(REFRESH_TOKEN)에 저장된 리프레시 토큰으로 리프레시 토큰과 엑세스 토큰을 재발급합니다.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "성공",
        content = @Content(schema = @Schema(implementation = JwtDto.class))),
    @ApiResponse(
        responseCode = "401",
        description = "인증 오류",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(
        responseCode = "500",
        description = "서버 오류",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  JwtDto reissueToken(
      @Parameter(description = "리프레시 토큰") String refreshToken, HttpServletResponse response);

  @Operation(summary = "비밀번호 초기화", description = "임시 비밀번호로 초기화 후 이메일로 전송합니다.")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "성공"),
    @ApiResponse(
        responseCode = "400",
        description = "잘못된 요청",
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
  ResponseEntity<Void> resetPassword(ResetPasswordRequest resetPasswordRequest);
}
