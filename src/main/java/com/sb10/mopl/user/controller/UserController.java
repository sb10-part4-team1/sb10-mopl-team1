package com.sb10.mopl.user.controller;

import com.sb10.mopl.user.dto.request.UserCreateRequest;
import com.sb10.mopl.user.dto.response.UserDto;
import com.sb10.mopl.user.service.UserService;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  @PostMapping
  public ResponseEntity<UserDto> signUp(@Valid @RequestBody UserCreateRequest userCreateRequest) {
    UserDto userDto = userService.signUp(userCreateRequest);
    return ResponseEntity.created(URI.create("/api/users/" + userDto.id())).body(userDto);
  }
}
