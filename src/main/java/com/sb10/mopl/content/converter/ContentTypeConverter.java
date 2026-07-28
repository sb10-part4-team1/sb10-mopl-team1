package com.sb10.mopl.content.converter;

import com.sb10.mopl.content.entity.ContentType;
import com.sb10.mopl.content.exception.ContentErrorCode;
import com.sb10.mopl.content.exception.ContentException;
import java.util.Arrays;
import java.util.Map;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class ContentTypeConverter implements Converter<String, ContentType> {

  @Override
  public ContentType convert(String source) {
    if (source == null || source.isBlank()) {
      return null;
    }
    String trimmed = source.trim();
    return Arrays.stream(ContentType.values())
        .filter(
            type ->
                type.getValue().equalsIgnoreCase(trimmed) || type.name().equalsIgnoreCase(trimmed))
        .findFirst()
        .orElseThrow(
            () ->
                new ContentException(
                    ContentErrorCode.INVALID_CONTENT_TYPE, Map.of("rejectedValue", source)));
  }
}
