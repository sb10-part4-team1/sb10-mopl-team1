package com.sb10.mopl.auth.email;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MailTemporaryPasswordEmailSenderTest {

  private static final String FROM_ADDRESS = "mopl-noreply@example.com";
  private static final Session MAIL_SESSION = Session.getInstance(new Properties());

  @Mock private JavaMailSender javaMailSender;

  private MailTemporaryPasswordEmailSender sender;

  @BeforeEach
  void setUp() {
    sender = new MailTemporaryPasswordEmailSender(javaMailSender);
    ReflectionTestUtils.setField(sender, "from", FROM_ADDRESS);
    sender.loadHtmlTemplate();
  }

  @Test
  @DisplayName("정상 요청이면 제목/수신자/발신자를 설정하고 평문과 이스케이프된 HTML 본문으로 메일을 발송한다")
  void send_success_whenRequestIsValid() throws Exception {
    // given
    MimeMessage mimeMessage = new MimeMessage(MAIL_SESSION);
    when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
    String temporaryPassword = "Ab<1>Cd!";

    // when
    sender.send("user@example.com", temporaryPassword);

    // then
    verify(javaMailSender).send(mimeMessage);
    // 실제 전송 시 JavaMailSenderImpl이 내부적으로 호출하는 saveChanges()를 호출해
    // 각 MIME 파트의 Content-Type 헤더를 확정한 뒤 본문을 검사한다.
    mimeMessage.saveChanges();
    List<String> plainTexts = new ArrayList<>();
    List<String> htmlTexts = new ArrayList<>();
    collectTextParts(mimeMessage, plainTexts, htmlTexts);

    assertEquals("[모두의 플리] 임시 비밀번호가 발급되었습니다.", mimeMessage.getSubject());
    assertEquals(FROM_ADDRESS, mimeMessage.getFrom()[0].toString());
    assertEquals("user@example.com", mimeMessage.getAllRecipients()[0].toString());
    assertTrue(plainTexts.stream().anyMatch(text -> text.contains(temporaryPassword)));
    assertTrue(htmlTexts.stream().anyMatch(text -> text.contains("Ab&lt;1&gt;Cd!")));
    assertTrue(htmlTexts.stream().noneMatch(text -> text.contains(temporaryPassword)));
  }

  @Test
  @DisplayName("메일 생성 중 MessagingException이 발생하면 MailPreparationException으로 변환하고 발송하지 않는다")
  void send_throwsMailPreparationException_whenMessagingExceptionOccurs() {
    // given
    MimeMessage mimeMessage = new MimeMessage(MAIL_SESSION);
    when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
    String invalidRecipient = "victim@example.com\r\nBcc: attacker@example.com";

    // when & then
    assertThrows(MailPreparationException.class, () -> sender.send(invalidRecipient, "Temp1234!"));
    verify(javaMailSender, never()).send(any(MimeMessage.class));
  }

  private void collectTextParts(Part part, List<String> plainTexts, List<String> htmlTexts)
      throws Exception {
    Object content = part.getContent();
    if (content instanceof Multipart multipart) {
      for (int i = 0; i < multipart.getCount(); i++) {
        collectTextParts(multipart.getBodyPart(i), plainTexts, htmlTexts);
      }
    } else if (content instanceof String text) {
      if (part.isMimeType("text/html")) {
        htmlTexts.add(text);
      } else {
        plainTexts.add(text);
      }
    }
  }
}
