package com.sb10.mopl.playlist.event;

import java.util.UUID;

public record PlaylistCreatedEvent(
    UUID ownerId, String ownerName, String playlistTitle, String playlistDescription) {}
