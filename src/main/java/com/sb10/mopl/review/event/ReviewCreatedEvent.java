package com.sb10.mopl.review.event;

import java.util.UUID;

public record ReviewCreatedEvent(UUID reviewId, UUID reviewerId, UUID contentId) {}
