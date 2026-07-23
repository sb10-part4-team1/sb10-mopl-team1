package com.sb10.mopl.follow.controller;

import com.sb10.mopl.auth.security.user.AuthenticatedUser;
import com.sb10.mopl.auth.security.user.CurrentUser;
import com.sb10.mopl.follow.controller.api.FollowControllerApiDocs;
import com.sb10.mopl.follow.dto.FollowDto;
import com.sb10.mopl.follow.dto.FollowRequest;
import com.sb10.mopl.follow.service.FollowService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/follows")
public class FollowController implements FollowControllerApiDocs {

  private final FollowService followService;

  @Override
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public FollowDto follow(
      @CurrentUser AuthenticatedUser currentUser, @Valid @RequestBody FollowRequest request) {
    return followService.follow(currentUser.id(), request);
  }

  @Override
  @DeleteMapping("/{followId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void unfollow(@CurrentUser AuthenticatedUser currentUser, @PathVariable UUID followId) {
    followService.unfollow(currentUser.id(), followId);
  }

  @Override
  @GetMapping("/followed-by-me")
  public FollowDto findFollowedByMe(
      @CurrentUser AuthenticatedUser currentUser, @RequestParam UUID followeeId) {
    return followService.findFollowedByMe(currentUser.id(), followeeId);
  }

  @Override
  @GetMapping("/count")
  public long countFollowers(@RequestParam UUID followeeId) {
    return followService.countFollowers(followeeId);
  }
}
