package com.sb10.mopl.user.mapper;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.sb10.mopl.user.dto.response.UserDto;
import com.sb10.mopl.user.entity.User;
import com.sb10.mopl.user.entity.UserRole;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

class UserMapperTest {

  private final UserMapper userMapper = Mappers.getMapper(UserMapper.class);

  @Test
  @DisplayName("User 엔티티를 UserDto로 변환한다")
  void toDto_success_whenUserIsValid() {
    // given
    UUID userId = UUID.randomUUID();
    Instant createdAt = Instant.parse("2026-06-24T00:00:00Z");
    User user =
        User.createUser(
            "test-user", "user@example.com", "encoded-password", "https://image.example.com");
    user.changeLocked(true);
    ReflectionTestUtils.setField(user, "id", userId);
    ReflectionTestUtils.setField(user, "createdAt", createdAt);

    // when
    UserDto userDto = userMapper.toDto(user);

    // then
    assertAll(
        () -> assertEquals(userId, userDto.id()),
        () -> assertEquals(createdAt, userDto.createdAt()),
        () -> assertEquals("user@example.com", userDto.email()),
        () -> assertEquals("test-user", userDto.name()),
        () -> assertEquals("https://image.example.com", userDto.profileImageUrl()),
        () -> assertEquals(UserRole.USER, userDto.role()),
        () -> assertEquals(true, userDto.locked()));
  }
}
