package com.sb10.mopl.review.dto;

import java.util.UUID;

// 리뷰 작성자를 표현하기 위한 DTO(응답구조를 맞추기위해 구현)
public record ReviewAuthorDto(UUID userId, String name, String profileImageUrl) {}
