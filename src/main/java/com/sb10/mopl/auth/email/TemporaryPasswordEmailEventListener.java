package com.sb10.mopl.auth.email;

import com.sb10.mopl.auth.event.TemporaryPasswordIssuedEvent;
import com.sb10.mopl.auth.service.TemporaryPasswordService;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class TemporaryPasswordEmailEventListener {

  private static final int MAX_ATTEMPTS = 3;
  private static final Duration RETRY_BACKOFF = Duration.ofSeconds(1);

  private final TemporaryPasswordEmailSender temporaryPasswordEmailSender;
  private final TemporaryPasswordService temporaryPasswordService;

  @Async("ioExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void sendTemporaryPassword(TemporaryPasswordIssuedEvent event) {
    try {
      sendWithRetry(event);
    } catch (RuntimeException exception) {
      log.error(
          "임시 비밀번호 메일 발송 최종 실패 - userId: {}, temporaryPasswordId: {}, email: {}",
          event.userId(),
          event.temporaryPasswordId(),
          event.email(),
          exception);
      compensate(event);
    }
  }

  private void sendWithRetry(TemporaryPasswordIssuedEvent event) {
    RuntimeException lastException = null;
    for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
      try {
        temporaryPasswordEmailSender.send(event.email(), event.temporaryPassword());
        return;
      } catch (RuntimeException exception) {
        lastException = exception;
        log.warn(
            "임시 비밀번호 메일 발송 실패 - attempt: {}/{}, userId: {}, email: {}",
            attempt,
            MAX_ATTEMPTS,
            event.userId(),
            event.email(),
            exception);
        sleepBeforeRetry(attempt);
      }
    }
    throw lastException;
  }

  private void sleepBeforeRetry(int attempt) {
    if (attempt >= MAX_ATTEMPTS) {
      return;
    }
    try {
      Thread.sleep(RETRY_BACKOFF.toMillis());
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("임시 비밀번호 메일 발송 재시도 대기가 중단되었습니다.", exception);
    }
  }

  private void compensate(TemporaryPasswordIssuedEvent event) {
    try {
      temporaryPasswordService.deleteIssuedTemporaryPassword(
          event.userId(), event.temporaryPasswordId());
    } catch (RuntimeException exception) {
      log.error(
          "임시 비밀번호 메일 발송 실패 보상 처리 실패 - userId: {}, temporaryPasswordId: {}",
          event.userId(),
          event.temporaryPasswordId(),
          exception);
    }
  }
}
