package com.sb10.mopl.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SignInRequest(
    @Schema(description = "이메일")
        @Email(
            regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
            message = "올바른 이메일 형식이어야 합니다.")
        @NotBlank
        String username,
    @Schema(description = "비밀번호") @NotBlank String password) {}
