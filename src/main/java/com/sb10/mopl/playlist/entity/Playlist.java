package com.sb10.mopl.playlist.entity;

import com.sb10.mopl.common.entity.BaseUpdatableEntity;
import com.sb10.mopl.common.validation.DomainValidator;
import com.sb10.mopl.playlist.exception.PlaylistErrorCode;
import com.sb10.mopl.playlist.exception.PlaylistException;
import com.sb10.mopl.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
    name = "playlists",
    indexes = {@Index(name = "idx_playlists_owner_id", columnList = "owner_id")})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Playlist extends BaseUpdatableEntity {

  // 플레이리스트를 만든 유저의 id
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "owner_id", nullable = false)
  private User owner;

  // 플레이리스트 제목
  @Column(name = "title", nullable = false, length = 255)
  private String title;

  // 플레이리스트 설명
  @Column(name = "description", nullable = false, columnDefinition = "TEXT")
  private String description;

  private static void validateCreate(User owner, String title, String description) {
    DomainValidator.start()
        .check(owner == null, "owner", "플레이리스트 제작자는 필수입니다.")
        .check(title == null || title.isBlank(), "title", "플레이리스트 제목은 필수입니다.")
        .check(title != null && title.length() > 255, "title", "플레이리스트 제목은 255자 이하여야 합니다.")
        .check(description == null || description.isBlank(), "description", "플레이리스트 설명은 필수입니다.")
        .orThrow(
            details -> new PlaylistException(PlaylistErrorCode.INVALID_PLAYLIST_VALUE, details));
  }

  public Playlist(User owner, String title, String description) {
    validateCreate(owner, title, description);
    this.owner = owner;
    this.title = title;
    this.description = description;
  }
}
