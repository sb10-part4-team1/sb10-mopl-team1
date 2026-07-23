package com.sb10.mopl.content.entity;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "contents")
public class ContentDocument {

  @Id
  @Field(type = FieldType.Keyword)
  private String id;

  @Field(type = FieldType.Text, analyzer = "standard")
  private String title;

  @Field(type = FieldType.Keyword)
  private String type;

  @Field(type = FieldType.Text, analyzer = "standard")
  private String description;

  @Field(type = FieldType.Double)
  private double averageRating;

  @Field(type = FieldType.Integer)
  private int reviewCount;

  @Field(type = FieldType.Long)
  private long watcherCount;

  @Field(type = FieldType.Keyword)
  private String createdAt;

  public static ContentDocument from(Content content) {
    return ContentDocument.builder()
        .id(content.getId().toString())
        .title(content.getTitle())
        .type(content.getType() != null ? content.getType().name() : null)
        .description(content.getDescription())
        .averageRating(content.getAverageRating())
        .reviewCount(content.getReviewCount())
        .watcherCount(content.getWatcherCount())
        .createdAt(content.getCreatedAt() != null ? content.getCreatedAt().toString() : null)
        .build();
  }

  public UUID getUuidId() {
    return UUID.fromString(id);
  }
}
