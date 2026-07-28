package com.sb10.mopl.content.scheduler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sb10.mopl.content.service.ContentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContentWatcherCountSyncSchedulerTest {

  @Mock private ContentService contentService;

  @InjectMocks private ContentWatcherCountSyncScheduler scheduler;

  @Nested
  @DisplayName("1. ContentWatcherCountSyncScheduler 스케줄러 정상 동작 및 예외 방어 검증")
  class SyncWatcherCountTests {

    @Test
    @DisplayName("스케줄러 실행 시 콘텐츠 시청자 수 정합성 보정 서비스 메서드가 정상 호출된다")
    void syncWatcherCount_success_whenServiceReturnsUpdatedCount() {
      // given: contentService가 보정 건수 10건을 반환하도록 설정한다
      when(contentService.syncWatcherCount()).thenReturn(10);

      // when: 스케줄러의 syncWatcherCount 메서드를 실행할 때
      scheduler.syncWatcherCount();

      // that: contentService의 syncWatcherCount 메서드가 정확히 1회 호출되었는지 검증한다
      verify(contentService).syncWatcherCount();
    }

    @Test
    @DisplayName("보정 서비스 실행 중 예외가 발생하더라도 스케줄러 내부에서 예외를 처리하여 외부로 던지지 않는다")
    void syncWatcherCount_doesNotThrow_whenServiceThrowsException() {
      // given: contentService 호출 시 런타임 예외가 발생하도록 설정한다
      when(contentService.syncWatcherCount()).thenThrow(new RuntimeException("DB 연결 장애"));

      // when & that: 스케줄러 실행 시 예외가 발생하지 않고 안전하게 처리되는지 검증한다
      assertDoesNotThrow(() -> scheduler.syncWatcherCount());
      verify(contentService).syncWatcherCount();
    }
  }
}
