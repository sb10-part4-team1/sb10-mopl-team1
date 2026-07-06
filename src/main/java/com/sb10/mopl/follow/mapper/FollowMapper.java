package com.sb10.mopl.follow.mapper;

import com.sb10.mopl.follow.dto.FollowCreateRequest;
import com.sb10.mopl.follow.dto.FollowDto;
import com.sb10.mopl.follow.entity.Follow;
import java.util.UUID;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FollowMapper {

  default Follow toEntity(UUID followerId, FollowCreateRequest request) {
    return new Follow(followerId, request.followeeId());
  }

  // 필드명이 동일하여 @Mapping 생략
  FollowDto toDto(Follow follow);
}
