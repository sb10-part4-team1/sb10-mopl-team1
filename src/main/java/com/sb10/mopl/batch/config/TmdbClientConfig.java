package com.sb10.mopl.batch.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class TmdbClientConfig {

  @Value("${mopl.tmdb.api.token}")
  private String accessToken;

  @Value("${mopl.tmdb.api.base-url}")
  private String baseUrl;

  @Bean
  public RestClient tmdbRestClient(
      RestClient.Builder builder, ClientHttpRequestFactory requestFactory) {
    return builder
        .requestFactory(requestFactory)
        .baseUrl(baseUrl)
        .defaultHeader("Authorization", "Bearer " + accessToken)
        .defaultHeader("Accept", "application/json")
        .build();
  }
}
