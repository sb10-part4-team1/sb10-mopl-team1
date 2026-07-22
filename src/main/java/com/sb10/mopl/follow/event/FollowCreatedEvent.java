package com.sb10.mopl.follow.event;

import java.util.UUID;

public record FollowCreatedEvent(UUID followerId, UUID followeeId) {}
