package com.sb10.mopl.user.mapper;

import com.sb10.mopl.user.dto.UserDto;
import com.sb10.mopl.user.dto.UserSummary;
import com.sb10.mopl.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

  UserDto toDto(User user);

  @Mapping(target = "userId", source = "id")
  UserSummary toSummary(User user);
}
