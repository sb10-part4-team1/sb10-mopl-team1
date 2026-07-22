package com.sb10.mopl.playlistcontent.event;

import java.util.UUID;

public record PlaylistContentAddedEvent(
    UUID playlistId, String playlistTitle, UUID contentId, UUID ownerId) {}
