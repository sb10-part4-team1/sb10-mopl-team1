package com.sb10.mopl.batch.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

@Configuration
public class RestClientCommonConfig {

  @Value("${mopl.external-api.connect-timeout-ms:3000}")
  private int connectTimeoutMs;

  @Value("${mopl.external-api.read-timeout-ms:5000}")
  private int readTimeoutMs;

  @Bean
  public ClientHttpRequestFactory externalApiRequestFactory() {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(connectTimeoutMs);
    factory.setReadTimeout(readTimeoutMs);
    return factory;
  }
}
