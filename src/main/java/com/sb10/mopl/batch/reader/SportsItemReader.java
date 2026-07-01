package com.sb10.mopl.batch.reader;

import com.sb10.mopl.batch.client.SportsApiClient;
import com.sb10.mopl.batch.dto.SportsApiResponse;
import com.sb10.mopl.batch.dto.SportsContentDto;
import com.sb10.mopl.batch.job.League;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 어제 일자 혹은 수동 지정 날짜의 모든 리그(League Enum 10개) 스포츠 경기 데이터를 SportsDB API로부터 수집하는 리더입니다. StepScope를 통해 매
 * 실행 시점마다 어제 날짜를 기준으로 동적으로 동작합니다.
 */
@Slf4j
@Component
@StepScope
public class SportsItemReader implements ItemReader<SportsContentDto> {

  private final SportsApiClient sportsApiClient;
  private final String targetDate;

  private List<SportsContentDto> buffer = new ArrayList<>();
  private int index = 0;
  private boolean isLoaded = false;

  public SportsItemReader(
      SportsApiClient sportsApiClient, @Value("#{jobParameters['targetDate']}") String targetDate) {
    this.sportsApiClient = sportsApiClient;
    this.targetDate = targetDate;
  }

  @Override
  public SportsContentDto read() {
    if (!isLoaded) {
      fetchEvents();
      isLoaded = true;
    }

    if (index >= buffer.size()) {
      return null; // 데이터 수집 완료
    }

    return buffer.get(index++);
  }

  private void fetchEvents() {
    // targetDate 파라미터가 유효하면 사용하고, 없으면 기본값인 어제 날짜로 설정
    String date =
        StringUtils.hasText(this.targetDate)
            ? this.targetDate
            : LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE);

    log.info("SportsDB 전체 리그 수집 시작 - date: {}", date);

    List<SportsContentDto> allEvents = new ArrayList<>();

    // League Enum에 등록된 모든 리그를 루프 돌며 순차 수집
    for (League league : League.values()) {
      log.info("SportsDB API 호출 - leagueId: {} ({})", league.getLeagueId(), league.getLeagueName());
      SportsApiResponse response = sportsApiClient.fetchEventsByDay(date, league.getLeagueId());

      // 1. API 응답 구조 붕괴 상황에 대한 명확한 예외 처리 (events 키가 아예 없음)
      if (response == null || !response.isHasEventsKey()) {
        throw new IllegalStateException(
            "SportsDB API 응답 구조가 변경되었거나 비정상입니다. 'events' 필드가 유실되었습니다. 리그: "
                + league.getLeagueName());
      }

      // 2. 정상적으로 해당 날짜에 경기가 없는 경우 (events: null)
      if (response.isEmpty()) {
        log.info("{} 리그는 해당 날짜에 경기가 존재하지 않습니다. (events: null)", league.getLeagueName());
        continue;
      }

      allEvents.addAll(response.getEvents());
    }

    this.buffer = allEvents;
    log.info("SportsDB 전체 수집 완료 - 총 {} 건", this.buffer.size());
    this.index = 0;
  }
}
