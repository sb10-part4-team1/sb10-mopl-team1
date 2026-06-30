package com.sb10.mopl.batch.sports.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SportsContentDto(
    String idEvent, // 식별자로 사용할 수도 있음(예비)
    String strEvent, // Title
    String strFilename, // Description
    String strThumb, // ThumbnailUrl
    String strVenue // Tag
    ) {}
