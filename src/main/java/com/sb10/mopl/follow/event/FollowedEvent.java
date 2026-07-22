package com.sb10.mopl.follow.event;

import java.util.UUID;

public record FollowedEvent(UUID followeeId, String followerName) {}
