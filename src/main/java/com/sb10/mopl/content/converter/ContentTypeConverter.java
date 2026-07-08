package com.sb10.mopl.content.converter;

import com.sb10.mopl.content.entity.ContentType;
import java.util.Arrays;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class ContentTypeConverter implements Converter<String, ContentType> {

  @Override
  public ContentType convert(String source) {
    if (source.isBlank()) {
      return null;
    }
    String trimmed = source.trim();
    return Arrays.stream(ContentType.values())
        .filter(
            type ->
                type.getValue().equalsIgnoreCase(trimmed) || type.name().equalsIgnoreCase(trimmed))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 콘텐츠 타입입니다: " + source));
  }
}
