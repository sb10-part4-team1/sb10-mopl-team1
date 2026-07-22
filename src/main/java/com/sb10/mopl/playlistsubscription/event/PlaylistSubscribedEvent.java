package com.sb10.mopl.playlistsubscription.event;

import java.util.UUID;

public record PlaylistSubscribedEvent(UUID subscriberId, UUID playlistId, UUID ownerId) {}
