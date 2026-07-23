package com.sb10.mopl.content.dto;

import java.util.List;
import java.util.UUID;

public record EsSearchResult(List<UUID> ids, long totalHits) {}
