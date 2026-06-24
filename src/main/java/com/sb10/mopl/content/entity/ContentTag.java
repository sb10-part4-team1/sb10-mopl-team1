package com.sb10.mopl.content.entity;

import com.sb10.mopl.common.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "content_tags",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "UQ_CONTENT_TAGS",
          columnNames = {"content_id", "tag_id"})
    })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContentTag extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "content_id", nullable = false)
  private Content content;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tag_id", nullable = false)
  private Tag tag;

  private ContentTag(Content content, Tag tag) {
    this.content = content;
    this.tag = tag;
    if (content != null) {
      content.getContentTags().add(this);
    }
  }

  public static ContentTag create(Content content, Tag tag) {
    return new ContentTag(content, tag);
  }
}
