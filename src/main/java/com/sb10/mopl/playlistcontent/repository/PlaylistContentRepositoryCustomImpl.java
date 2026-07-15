package com.sb10.mopl.playlistcontent.repository;

import static com.sb10.mopl.playlistcontent.entity.QPlaylistContent.playlistContent;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sb10.mopl.playlistcontent.entity.PlaylistContent;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PlaylistContentRepositoryCustomImpl implements PlaylistContentRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  // 플레이리스트에 동일한 콘텐츠가 이미 존재하는지 확인
  @Override
  public boolean existsByPlaylistIdAndContentId(UUID playlistId, UUID contentId) {
    Integer result =
        queryFactory
            .selectOne()
            .from(playlistContent)
            .where(
                playlistContent.playlist.id.eq(playlistId),
                playlistContent.content.id.eq(contentId))
            .fetchFirst();

    return result != null;
  }

  // 플레이리스트 ID와 콘텐츠 ID로 플레이리스트 콘텐츠 조회
  @Override
  public Optional<PlaylistContent> findByPlaylistIdAndContentId(UUID playlistId, UUID contentId) {

    PlaylistContent result =
        queryFactory
            .selectFrom(playlistContent)
            .where(
                playlistContent.playlist.id.eq(playlistId),
                playlistContent.content.id.eq(contentId))
            .fetchOne();

    return Optional.ofNullable(result);
  }
}
