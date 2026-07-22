package com.sb10.mopl.playlistsubscription.event;

import java.util.UUID;

public record PlaylistSubscribedEvent(
    UUID ownerId, String subscriberName, UUID playlistId, String playlistTitle) {}
