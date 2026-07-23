package com.sb10.mopl.config;

import com.sb10.mopl.auth.security.principal.CurrentUserArgumentResolver;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

  private final CurrentUserArgumentResolver currentUserArgumentResolver;

  @Value("${mopl.upload-dir}")
  private String uploadDir;

  @Override
  public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
    resolvers.add(currentUserArgumentResolver);
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    String location =
        uploadDir.startsWith("/") || uploadDir.contains(":")
            ? "file:" + uploadDir
            : "file:" + System.getProperty("user.dir") + "/" + uploadDir;

    // 만약 경로 끝에 "/" 가 누락되어 있다면 자동으로 보정하여 스프링 리소스 로더 정합성 유지
    if (!location.endsWith("/")) {
      location += "/";
    }

    registry.addResourceHandler("/uploads/**").addResourceLocations(location);
  }
}
