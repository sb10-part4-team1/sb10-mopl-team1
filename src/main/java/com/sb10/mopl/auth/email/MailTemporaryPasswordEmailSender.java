package com.sb10.mopl.auth.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.util.HtmlUtils;

@Component
@Profile("!test")
@RequiredArgsConstructor
public class MailTemporaryPasswordEmailSender implements TemporaryPasswordEmailSender {

  private static final String SUBJECT = "[모두의 플리] 임시 비밀번호가 발급되었습니다.";
  private static final String TEMPLATE_LOCATION = "templates/mail/temporary-password.html";
  private static final String TEMPORARY_PASSWORD_PLACEHOLDER = "{{temporaryPassword}}";

  private final JavaMailSender javaMailSender;

  @Value("${mopl.mail.temporary-password.from}")
  private String from;

  @Override
  public void send(String email, String temporaryPassword) {
    try {
      MimeMessage message = javaMailSender.createMimeMessage();
      MimeMessageHelper helper =
          new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
      helper.setFrom(from);
      helper.setTo(email);
      helper.setSubject(SUBJECT);
      helper.setText(buildPlainTextContent(temporaryPassword), buildHtmlContent(temporaryPassword));
      javaMailSender.send(message);
    } catch (MessagingException e) {
      throw new MailPreparationException("임시 비밀번호 메일 생성에 실패했습니다.", e);
    }
  }

  private String buildPlainTextContent(String temporaryPassword) {
    return """
        [MOPL] 임시 비밀번호 안내

        요청하신 MOPL 계정의 임시 비밀번호가 발급되었습니다.

        임시 비밀번호: %s

        이 비밀번호는 발급 후 3분 동안만 사용할 수 있습니다.
        로그인 후에는 반드시 새로운 비밀번호로 변경해 주세요.

        본인이 요청하지 않았다면 이 메일을 무시해 주세요.
        """
        .formatted(temporaryPassword);
  }

  private String buildHtmlContent(String temporaryPassword) {
    String escapedTemporaryPassword = HtmlUtils.htmlEscape(temporaryPassword);
    return loadTemplate().replace(TEMPORARY_PASSWORD_PLACEHOLDER, escapedTemporaryPassword);
  }

  private String loadTemplate() {
    ClassPathResource resource = new ClassPathResource(TEMPLATE_LOCATION);
    try (InputStream inputStream = resource.getInputStream()) {
      return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new MailPreparationException("임시 비밀번호 메일 템플릿을 읽지 못했습니다.", e);
    }
  }
}
