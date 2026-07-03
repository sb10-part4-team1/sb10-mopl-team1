package com.sb10.mopl.playlistcontent.entity;

import com.sb10.mopl.common.entity.BaseEntity;
import com.sb10.mopl.common.validation.DomainValidator;
import com.sb10.mopl.content.entity.Content;
import com.sb10.mopl.playlist.entity.Playlist;
import com.sb10.mopl.playlistcontent.exception.PlaylistContentErrorCode;
import com.sb10.mopl.playlistcontent.exception.PlaylistContentException;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
    name = "playlist_contents",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "UQ_PLAYLIST_CONTENTS",
          columnNames = {"playlist_id", "content_id"})
    })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaylistContent extends BaseEntity {

  // 플레이 리스트 객체
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "playlist_id", nullable = false)
  private Playlist playlist;

  // 콘텐츠 객체
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "content_id", nullable = false)
  private Content content;

  private static void validateCreate(Playlist playlist, Content content) {
    DomainValidator.start()
        .check(playlist == null, "playlist", "플레이리스트는 필수입니다.")
        .check(content == null, "content", "콘텐츠는 필수입니다.")
        .orThrow(
            details ->
                new PlaylistContentException(
                    PlaylistContentErrorCode.INVALID_PLAYLIST_CONTENT_VALUE, details));
  }

  public PlaylistContent(Playlist playlist, Content content) {
    validateCreate(playlist, content);
    this.playlist = playlist;
    this.content = content;
  }
}
