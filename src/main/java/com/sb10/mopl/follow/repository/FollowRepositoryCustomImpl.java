package com.sb10.mopl.follow.repository;

import static com.sb10.mopl.follow.entity.QFollow.follow;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FollowRepositoryCustomImpl implements FollowRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  // 특정 사용자를 팔로우하는 사용자 ID 목록을 커서 기반으로 조회
  @Override
  public List<UUID> findFollowerIdsByFolloweeId(UUID followeeId, UUID idAfter, int limit) {

    return queryFactory
        .select(follow.followerId)
        .from(follow)
        .where(follow.followeeId.eq(followeeId), followerIdAfter(idAfter))
        .orderBy(follow.followerId.asc())
        .limit(limit)
        .fetch();
  }

  private BooleanExpression followerIdAfter(UUID idAfter) {
    return idAfter != null ? follow.followerId.gt(idAfter) : null;
  }
}
