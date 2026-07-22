package com.sb10.mopl.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

  @Bean
  public OpenAPI openApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("모두의 플리 API 문서")
                .description("모두의 플리 프로젝트의 Swagger API 문서입니다.")
                .version("v1.0.0"));
  }
}
