package com.sb10.mopl.auth.util;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
public class TemporaryPasswordGenerator {

  private static final int PASSWORD_LENGTH = 12;
  private static final char[] LOWERCASE = "abcdefghijklmnopqrstuvwxyz".toCharArray();
  private static final char[] UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
  private static final char[] DIGITS = "0123456789".toCharArray();
  private static final char[] SPECIALS = "!@#$%^&*".toCharArray();
  private static final char[] ALL_CHARACTERS =
      "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*".toCharArray();

  private final SecureRandom secureRandom = new SecureRandom();

  public String generate() {
    char[] password = new char[PASSWORD_LENGTH];
    password[0] = randomCharacter(LOWERCASE);
    password[1] = randomCharacter(UPPERCASE);
    password[2] = randomCharacter(DIGITS);
    password[3] = randomCharacter(SPECIALS);

    for (int i = 4; i < PASSWORD_LENGTH; i++) {
      password[i] = randomCharacter(ALL_CHARACTERS);
    }

    shuffle(password);
    return new String(password);
  }

  private char randomCharacter(char[] characters) {
    return characters[secureRandom.nextInt(characters.length)];
  }

  private void shuffle(char[] characters) {
    for (int i = characters.length - 1; i > 0; i--) {
      int j = secureRandom.nextInt(i + 1);
      char temporary = characters[i];
      characters[i] = characters[j];
      characters[j] = temporary;
    }
  }
}
