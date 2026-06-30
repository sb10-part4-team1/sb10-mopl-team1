package com.sb10.mopl.batch.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class SportsClientConfig {

  @Value("${mopl.sports.api.base-url}")
  private String baseUrl;

  @Bean
  public RestClient sportsRestClient(RestClient.Builder builder) {
    return builder.baseUrl(baseUrl).defaultHeader("Accept", "application/json").build();
  }
}
