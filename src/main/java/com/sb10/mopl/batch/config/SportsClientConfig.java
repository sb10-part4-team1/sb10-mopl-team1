package com.sb10.mopl.batch.config;

import com.sb10.mopl.batch.interceptor.MetricsClientHttpRequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class SportsClientConfig {

  @Value("${mopl.sports.api.base-url}")
  private String baseUrl;

  @Bean
  public RestClient sportsRestClient(
      RestClient.Builder builder,
      ClientHttpRequestFactory requestFactory,
      MetricsClientHttpRequestInterceptor interceptor) {
    return builder
        .requestFactory(requestFactory)
        .baseUrl(baseUrl)
        .defaultHeader("Accept", "application/json")
        .requestInterceptor(interceptor)
        .build();
  }
}
