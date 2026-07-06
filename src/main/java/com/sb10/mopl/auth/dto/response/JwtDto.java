package com.sb10.mopl.auth.dto.response;

import com.sb10.mopl.user.dto.response.UserDto;

public record JwtDto(UserDto userDto, String accessToken) {}
