package com.sb10.mopl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@EnableRetry
@SpringBootApplication
public class MoplApplication {

  public static void main(String[] args) {
    SpringApplication.run(MoplApplication.class, args);
  }
}
