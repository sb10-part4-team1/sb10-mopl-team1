package com.sb10.mopl.user.mapper;

import com.sb10.mopl.user.dto.response.UserDto;
import com.sb10.mopl.user.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

  UserDto toDto(User user);
}
