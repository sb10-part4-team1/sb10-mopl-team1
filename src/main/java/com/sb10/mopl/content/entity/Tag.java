package com.sb10.mopl.content.entity;

import com.sb10.mopl.common.entity.BaseEntity;
import com.sb10.mopl.common.validation.DomainValidator;
import com.sb10.mopl.content.exception.ContentErrorCode;
import com.sb10.mopl.content.exception.ContentException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tags")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tag extends BaseEntity {

  @Column(name = "name", nullable = false, unique = true, length = 100)
  private String name;

  private Tag(String name) {
    validate(name);
    this.name = name;
  }

  public static Tag create(String name) {
    return new Tag(name);
  }

  private static void validate(String name) {
    DomainValidator.start()
        .check(name == null || name.isBlank(), "name", "태그 이름은 비어 있을 수 없습니다.")
        .check(name != null && name.length() > 100, "name", "태그 이름은 100자를 초과할 수 없습니다.")
        .orThrow(details -> new ContentException(ContentErrorCode.INVALID_CONTENT_DATA, details));
  }
}
