package com.sb10.mopl.auth.email;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.sb10.mopl.auth.event.TemporaryPasswordIssuedEvent;
import com.sb10.mopl.auth.service.TemporaryPasswordService;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TemporaryPasswordEmailEventListenerTest {

  @Mock private TemporaryPasswordEmailSender temporaryPasswordEmailSender;

  @Mock private TemporaryPasswordService temporaryPasswordService;

  @InjectMocks private TemporaryPasswordEmailEventListener listener;

  private final UUID userId = UUID.randomUUID();
  private final UUID temporaryPasswordId = UUID.randomUUID();
  private final TemporaryPasswordIssuedEvent event =
      new TemporaryPasswordIssuedEvent(
          userId, temporaryPasswordId, "user@example.com", "Temp1234!");

  @Test
  @DisplayName("첫 시도에 발송이 성공하면 재시도나 보상 삭제를 하지 않는다")
  void sendTemporaryPassword_success_whenFirstAttemptSucceeds() {
    // given & when
    listener.sendTemporaryPassword(event);

    // then
    verify(temporaryPasswordEmailSender, times(1)).send("user@example.com", "Temp1234!");
    verifyNoInteractions(temporaryPasswordService);
  }

  @Test
  @DisplayName("첫 시도가 실패해도 재시도에 성공하면 보상 삭제를 하지 않는다")
  void sendTemporaryPassword_success_afterRetry_whenFirstAttemptFails() {
    // given
    doThrow(new RuntimeException("mail server error"))
        .doNothing()
        .when(temporaryPasswordEmailSender)
        .send(anyString(), anyString());

    // when
    listener.sendTemporaryPassword(event);

    // then
    verify(temporaryPasswordEmailSender, times(2)).send("user@example.com", "Temp1234!");
    verifyNoInteractions(temporaryPasswordService);
  }

  @Test
  @DisplayName("최대 재시도 횟수를 모두 실패하면 발급된 임시 비밀번호를 보상 삭제한다")
  void sendTemporaryPassword_compensates_whenAllRetriesFail() {
    // given
    doThrow(new RuntimeException("mail server error"))
        .when(temporaryPasswordEmailSender)
        .send(anyString(), anyString());

    // when
    listener.sendTemporaryPassword(event);

    // then
    verify(temporaryPasswordEmailSender, times(3)).send("user@example.com", "Temp1234!");
    verify(temporaryPasswordService).deleteIssuedTemporaryPassword(userId, temporaryPasswordId);
  }

  @Test
  @DisplayName("보상 삭제 자체가 실패해도 예외를 전파하지 않는다")
  void sendTemporaryPassword_swallowsException_whenCompensateAlsoFails() {
    // given
    doThrow(new RuntimeException("mail server error"))
        .when(temporaryPasswordEmailSender)
        .send(anyString(), anyString());
    doThrow(new RuntimeException("compensate error"))
        .when(temporaryPasswordService)
        .deleteIssuedTemporaryPassword(userId, temporaryPasswordId);

    // when & then
    assertDoesNotThrow(() -> listener.sendTemporaryPassword(event));
    verify(temporaryPasswordService).deleteIssuedTemporaryPassword(userId, temporaryPasswordId);
  }
}
