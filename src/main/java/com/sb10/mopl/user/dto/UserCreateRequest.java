package com.sb10.mopl.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
    @Schema(description = "이름")
        @Size(min = 2, max = 50, message = "이름은 2자 이상 50자 이하여야 합니다.")
        @NotBlank(message = "이름은 필수입니다.")
        String name,
    @Schema(description = "이메일")
        @Email(
            regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
            message = "올바른 이메일 형식이어야 합니다.")
        @Size(min = 2, max = 255, message = "이메일은 2자 이상 255자 이하여야 합니다.")
        @NotBlank(message = "이메일은 필수입니다.")
        String email,
    @Schema(description = "비밀번호")
        @Size(min = 8, max = 255, message = "비밀번호는 8자 이상 255자 이하여야 합니다.")
        @NotBlank(message = "비밀번호는 필수입니다.")
        String password) {}
