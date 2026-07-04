package com.sb10.mopl.auth.email;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
@RequiredArgsConstructor
public class MailTemporaryPasswordEmailSender implements TemporaryPasswordEmailSender {

  private final JavaMailSender javaMailSender;

  @Value("${mopl.mail.temporary-password.from}")
  private String from;

  @Override
  public void send(String email, String temporaryPassword) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(from);
    message.setTo(email);
    message.setSubject("[MOPL] 임시 비밀번호 안내");
    message.setText("임시 비밀번호는 " + temporaryPassword + " 입니다. 3분 안에 로그인해 주세요.");
    javaMailSender.send(message);
  }
}
