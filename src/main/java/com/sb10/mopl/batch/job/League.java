package com.sb10.mopl.batch.job;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** SportsDB 무료 API로 조회할 리그 목록 eventsday.php?d={날짜}&l={leagueId} 로 사용 */
@Getter
@RequiredArgsConstructor
public enum League {
  EPL(4328, "English Premier League"),
  ENGLISH_CHAMPIONSHIP(4329, "English League Championship"),
  SCOTTISH_PREMIER(4330, "Scottish Premier League"),
  BUNDESLIGA(4331, "German Bundesliga"),
  SERIE_A(4332, "Italian Serie A"),
  LIGUE_1(4334, "French Ligue 1"),
  LA_LIGA(4335, "Spanish La Liga"),
  GREEK_SUPERLEAGUE(4336, "Greek Superleague Greece"),
  EREDIVISIE(4337, "Dutch Eredivisie"),
  BELGIAN_PRO_LEAGUE(4338, "Belgian Pro League");

  private final int leagueId;
  private final String leagueName;
}
