package com.sb10.mopl.watchingsession.mapper;

import com.sb10.mopl.content.mapper.ContentMapper;
import com.sb10.mopl.user.mapper.UserMapper;
import com.sb10.mopl.watchingsession.dto.WatchingSessionDto;
import com.sb10.mopl.watchingsession.entity.WatchingSession;
import org.mapstruct.Mapper;

@Mapper(
    componentModel = "spring",
    uses = {UserMapper.class, ContentMapper.class})
public interface WatchingSessionMapper {

  WatchingSessionDto toDto(WatchingSession watchingSession);
}
