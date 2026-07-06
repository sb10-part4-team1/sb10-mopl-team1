package com.sb10.mopl.auth.email;

public interface TemporaryPasswordEmailSender {

  void send(String email, String temporaryPassword);
}
