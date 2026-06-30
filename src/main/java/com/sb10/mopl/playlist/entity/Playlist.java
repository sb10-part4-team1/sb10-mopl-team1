package com.sb10.mopl.playlist.entity;

import com.sb10.mopl.common.entity.BaseUpdatableEntity;
import com.sb10.mopl.common.validation.DomainValidator;
import com.sb10.mopl.playlist.exception.PlaylistErrorCode;
import com.sb10.mopl.playlist.exception.PlaylistException;
import com.sb10.mopl.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "playlists")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Playlist extends BaseUpdatableEntity {

  // 플레이리스트를 만든 유저의 id
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "owner_id", nullable = false)
  private User owner;

  // 플레이리스트 제목
  @Column(name = "title", nullable = false, length = 100)
  private String title;

  // 플레이리스트 설명
  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  private static void validateCreate(User owner, String title) {
    DomainValidator.start()
        .check(owner == null, "owner", "플레이리스트 제작자는 필수입니다.")
        .check(title == null || title.isBlank(), "title", "플레이리스트 제목은 필수입니다.")
        .orThrow(
            details -> new PlaylistException(PlaylistErrorCode.INVALID_PLAYLIST_VALUE, details));
  }

  public Playlist(User owner, String title, String description) {
    validateCreate(owner, title);
    this.owner = owner;
    this.title = title;
    this.description = description;
  }
}
