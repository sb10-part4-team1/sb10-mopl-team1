package com.sb10.mopl.auth.dto;

import com.sb10.mopl.user.dto.UserDto;

public record JwtDto(UserDto userDto, String accessToken) {}
