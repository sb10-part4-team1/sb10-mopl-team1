package com.sb10.mopl.playlist.dto;

import java.util.UUID;

public record PlaylistOwnerDto(UUID userId, String name, String profileImageUrl) {}
