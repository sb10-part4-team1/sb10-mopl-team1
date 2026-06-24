package com.sb10.mopl.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
    @Size(min = 2, max = 50) @NotBlank String name,
    @Email @Size(min = 2, max = 255) @NotBlank String email,
    @Size(min = 8, max = 255) @NotBlank String password) {}
