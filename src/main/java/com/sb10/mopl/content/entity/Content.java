package com.sb10.mopl.content.entity;

import com.sb10.mopl.common.entity.BaseUpdatableEntity;
import com.sb10.mopl.common.validation.DomainValidator;
import com.sb10.mopl.content.exception.ContentErrorCode;
import com.sb10.mopl.content.exception.ContentException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "contents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Content extends BaseUpdatableEntity {

  @Column(name = "title", nullable = false, length = 100)
  private String title;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, length = 20)
  private ContentType type;

  @Column(name = "description", nullable = false, columnDefinition = "TEXT")
  private String description;

  @Column(name = "thumbnail_url", columnDefinition = "TEXT")
  private String thumbnailUrl;

  @Column(name = "average_rating", nullable = false)
  private double averageRating = 0.0;

  @Column(name = "review_count", nullable = false)
  private int reviewCount = 0;

  @Column(name = "watcher_count", nullable = false)
  private long watcherCount = 0L;

  @OneToMany(
      mappedBy = "content",
      fetch = FetchType.LAZY,
      cascade = CascadeType.ALL,
      orphanRemoval = true)
  private List<ContentTag> contentTags = new ArrayList<>();

  private Content(String title, ContentType type, String description, String thumbnailUrl) {
    validate(title, type, description);
    this.title = title;
    this.type = type;
    this.description = description;
    this.thumbnailUrl = thumbnailUrl;
  }

  public static Content create(
      String title, ContentType type, String description, String thumbnailUrl) {
    return new Content(title, type, description, thumbnailUrl);
  }

  private static void validate(String title, ContentType type, String description) {
    DomainValidator.start()
        .check(title == null || title.isBlank(), "title", "제목은 비어 있을 수 없습니다.")
        .check(title != null && title.length() > 100, "title", "제목은 100자를 초과할 수 없습니다.")
        .check(type == null, "type", "콘텐츠 형식은 필수 항목입니다.")
        .check(description == null || description.isBlank(), "description", "설명은 비어 있을 수 없습니다.")
        .orThrow(details -> new ContentException(ContentErrorCode.INVALID_CONTENT_DATA, details));
  }
}
