package com.sb10.mopl.batch.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.sb10.mopl.batch.client.SportsApiClient;
import com.sb10.mopl.batch.dto.SportsApiResponse;
import com.sb10.mopl.batch.dto.SportsContentDto;
import com.sb10.mopl.batch.exception.BatchException;
import com.sb10.mopl.batch.mapper.SportsContentMapper;
import com.sb10.mopl.content.entity.Content;
import com.sb10.mopl.content.entity.ContentProvider;
import com.sb10.mopl.content.entity.ContentType;
import com.sb10.mopl.content.repository.ContentRepository;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class SportsJobTest {

  @Autowired private JobLauncher jobLauncher;

  @Autowired private Job sportsJob;

  @Autowired private ContentRepository contentRepository;

  @MockitoBean private SportsApiClient sportsApiClient;

  @Autowired private SportsContentMapper sportsContentMapper;

  @BeforeEach
  void setUp() {
    contentRepository.deleteAllInBatch();
  }

  @AfterEach
  void tearDown() {
    contentRepository.deleteAllInBatch();
  }

  @Test
  @DisplayName("스포츠 경기가 10개 리그를 돌며 수집될 때, events가 null인 리그가 섞여 있어도 에러 없이 필터링 및 적재에 성공한다")
  void sportsJob_collectDataWithFiltering_whenSomeEventsAreInvalidOrDuplicated() throws Exception {
    // given: EPL DTO 모킹용 리스트 구성
    SportsContentDto normalEvent =
        new SportsContentDto(
            "2267443", "Brighton vs Man United", "EPL 2026-05-24", "/path.jpg", "Stadium");
    SportsContentDto missingTitleEvent =
        new SportsContentDto("2267444", null, "Wolverhampton vs Burnley", "/path2.jpg", "Stadium");
    SportsContentDto missingIdEvent =
        new SportsContentDto(
            null, "Crystal Palace vs Arsenal", "EPL 2026-05-24", "/path3.jpg", "Stadium");
    SportsContentDto noThumbEvent =
        new SportsContentDto("2267445", "Chelsea vs Liverpool", "EPL 2026-05-24", null, "Stadium");

    // EPL (4328) API 응답 모킹
    SportsApiResponse eplResponse = SportsApiResponse.empty();
    org.springframework.test.util.ReflectionTestUtils.setField(
        eplResponse,
        "events",
        List.of(normalEvent, missingTitleEvent, missingIdEvent, noThumbEvent));
    when(sportsApiClient.fetchEventsByDay(anyString(), eq(League.EPL.getLeagueId())))
        .thenReturn(eplResponse);

    // EPL을 제외한 나머지 9개 리그에 대해 events: null (SportsApiResponse.empty()) 반환 모킹
    for (League league : League.values()) {
      if (league != League.EPL) {
        when(sportsApiClient.fetchEventsByDay(anyString(), eq(league.getLeagueId())))
            .thenReturn(SportsApiResponse.empty());
      }
    }

    // when: 1차 배치 잡 구동 (이제 leagueId 파라미터는 제거됨)
    JobExecution firstRun =
        jobLauncher.run(
            sportsJob,
            new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .addString("targetDate", "2026-05-24")
                .toJobParameters());

    // then: 1차 잡 성공 및 데이터 적재 검증
    assertThat(firstRun.getStatus()).isEqualTo(BatchStatus.COMPLETED);

    List<Content> savedFirst = contentRepository.findAll();
    // 제목 누락과 ID 누락 경기는 제외되어 총 2건 저장됨 (normalEvent, noThumbEvent)
    assertThat(savedFirst).hasSize(2);
    assertThat(savedFirst)
        .extracting(Content::getProviderId)
        .containsExactlyInAnyOrder("2267443", "2267445");

    // 썸네일 누락 시 기본 썸네일 경로로 대체되는지 검증
    Content noThumbSaved =
        savedFirst.stream()
            .filter(c -> c.getProviderId().equals("2267445"))
            .findFirst()
            .orElseThrow();
    assertThat(noThumbSaved.getThumbnailUrl()).isEqualTo("/uploads/default-thumbnail.png");
    assertThat(noThumbSaved.getProvider()).isEqualTo(ContentProvider.SPORTS_DB);
    assertThat(noThumbSaved.getType()).isEqualTo(ContentType.SPORT);

    // given (2차 실행용): 1차에 수집된 normalEvent와 신규 경기 등록 시뮬레이션
    SportsContentDto newEvent =
        new SportsContentDto(
            "2267446", "Tottenham vs Newcastle", "EPL 2026-05-24", "/path4.jpg", "Stadium");

    SportsApiResponse eplResponse2 = SportsApiResponse.empty();
    org.springframework.test.util.ReflectionTestUtils.setField(
        eplResponse2, "events", List.of(normalEvent, newEvent)); // normalEvent는 중복 데이터
    when(sportsApiClient.fetchEventsByDay(anyString(), eq(League.EPL.getLeagueId())))
        .thenReturn(eplResponse2);

    // when: 2차 배치 잡 구동
    JobExecution secondRun =
        jobLauncher.run(
            sportsJob,
            new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .addString("targetDate", "2026-05-24")
                .toJobParameters());

    // then: 2차 잡 성공 및 중복 제거 검증
    assertThat(secondRun.getStatus()).isEqualTo(BatchStatus.COMPLETED);

    List<Content> savedSecond = contentRepository.findAll();
    // 1차에서 수집된 2건 + 2차 신규 1건 = 총 3건 저장
    assertThat(savedSecond).hasSize(3);
    assertThat(savedSecond)
        .extracting(Content::getProviderId)
        .containsExactlyInAnyOrder("2267443", "2267445", "2267446");
  }

  @Test
  @DisplayName("스포츠 API 응답에 events 키 자체가 유실된 포맷 붕괴 상황이 오면 배치는 예외를 발생시키고 실패한다")
  void sportsJob_fail_whenApiFormatIsInvalid() throws Exception {
    // given: events 필드 파싱 여부(hasEventsKey)가 false인 비정상 응답 구성
    SportsApiResponse invalidResponse = new SportsApiResponse(); // hasEventsKey = false

    when(sportsApiClient.fetchEventsByDay(anyString(), anyInt())).thenReturn(invalidResponse);

    // when: 배치 잡 구동
    JobExecution run =
        jobLauncher.run(
            sportsJob,
            new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .addString("targetDate", "2026-05-24")
                .toJobParameters());

    // then: 배치가 실패하고 예외 원인에 BatchException이 있는지 검증
    assertThat(run.getStatus()).isEqualTo(BatchStatus.FAILED);
    List<Throwable> exceptions = run.getAllFailureExceptions();
    assertThat(exceptions).isNotEmpty();
    assertThat(exceptions.get(0).getCause())
        .isInstanceOf(BatchException.class)
        .hasMessageContaining("API 응답 구조가 변경되었거나 비정상입니다");
  }
}
