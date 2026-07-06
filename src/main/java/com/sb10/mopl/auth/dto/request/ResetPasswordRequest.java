package com.sb10.mopl.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
    @Email(
            regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
            message = "올바른 이메일 형식이어야 합니다.")
        @Size(min = 2, max = 255, message = "이메일은 2자 이상 255자 이하여야 합니다.")
        @NotBlank(message = "이메일은 필수입니다.")
        String email) {}
