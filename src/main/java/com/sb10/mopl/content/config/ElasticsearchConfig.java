package com.sb10.mopl.content.config;

import java.time.Duration;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;

@Configuration
public class ElasticsearchConfig extends ElasticsearchConfiguration {

  @Value("${spring.elasticsearch.uris:http://localhost:9200}")
  private String elasticsearchUri;

  @Override
  public ClientConfiguration clientConfiguration() {
    String hostAndPort = elasticsearchUri.replace("http://", "").replace("https://", "");

    return ClientConfiguration.builder()
        .connectedTo(hostAndPort)
        .withConnectTimeout(Duration.ofSeconds(5))
        .withSocketTimeout(Duration.ofSeconds(5))
        .withClientConfigurer(
            builder -> {
              if (builder instanceof HttpAsyncClientBuilder httpClientBuilder) {
                httpClientBuilder
                    .setMaxConnTotal(150) // t4g.micro / t4g.small 환경 안전 커넥션 수치 (150개)
                    .setMaxConnPerRoute(100); // 라우트당 안전 커넥션 수치 (100개)
              }
              return builder;
            })
        .build();
  }
}
