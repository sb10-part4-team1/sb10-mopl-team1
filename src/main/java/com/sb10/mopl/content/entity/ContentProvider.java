package com.sb10.mopl.content.entity;

/** 콘텐츠의 유입 및 등록 경로를 나타내는 제공처 열거형입니다. */
public enum ContentProvider {
  /** 외부 TMDB API를 통해 수집된 콘텐츠 */
  TMDB,

  /** 외부 SportsDB API를 통해 수집된 콘텐츠 */
  SPORTS_DB,

  /** 관리자가 어드민 시스템을 통해 수동 등록한 콘텐츠 */
  MANUAL
}
