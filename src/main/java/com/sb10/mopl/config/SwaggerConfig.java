package com.sb10.mopl.config;

import com.sb10.mopl.auth.dto.request.SignInRequest;
import com.sb10.mopl.auth.dto.response.JwtDto;
import com.sb10.mopl.common.exception.ErrorResponse;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.converter.ResolvedSchema;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.List;
import java.util.Map;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

  private static final String AUTH_TAG = "인증 관리";
  private static final String SECURITY_FILTER_DESCRIPTION = "SecurityFilterChain에서 처리합니다.";

  private static final String BEARER_AUTH = "BearerAuth";
  private static final String CSRF_TOKEN = "CsrfToken";

  @Bean
  public OpenAPI openApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("모두의 플리 API 문서")
                .description("모두의 플리 프로젝트의 Swagger API 문서입니다.")
                .version("1.0"))
        .components(
            new Components()
                .addSecuritySchemes(
                    BEARER_AUTH,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .description("JWT 액세스 토큰 (로그인 후 발급)")
                        .scheme("bearer")
                        .bearerFormat("JWT"))
                .addSecuritySchemes(
                    CSRF_TOKEN,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .description("CSRF 토큰 (GET /api/auth/csrf-token 호출 후 XSRF-TOKEN 쿠키 값)")
                        .name("X-XSRF-TOKEN")
                        .in(SecurityScheme.In.HEADER)))
        .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH).addList(CSRF_TOKEN));
  }

  /**
   * `/api/auth/sign-in`, `/api/auth/sign-out` 컨트롤러 메서드가 아니라 Spring Security Filter가 처리하므로
   * `@Operation` 어노테이션을 붙일 수 없다. 생성된 스펙에 두 경로를 수동으로 추가한다.
   */
  @Bean
  public OpenApiCustomizer authFilterPathsCustomizer() {
    return openApi -> {
      registerSchema(openApi, SignInRequest.class);
      registerSchema(openApi, JwtDto.class);
      registerSchema(openApi, ErrorResponse.class);

      openApi
          .getPaths()
          .addPathItem("/api/auth/sign-in", new PathItem().post(signInOperation()))
          .addPathItem("/api/auth/sign-out", new PathItem().post(signOutOperation()));
    };
  }

  /**
   * springdoc이 어노테이션 스캔으로 이미 등록해 둔 스키마는 덮어쓰지 않고, 아직 없는 스키마만 추가한다. {@link ModelConverters}로 직접
   * resolve한 스키마는 {@code type: object}가 비어 있을 수 있어 명시적으로 채워준다(비어 있으면 Swagger UI가 요청 바디를 필드별 폼 대신
   * JSON 예시 값으로만 표시함).
   */
  private void registerSchema(OpenAPI openApi, Class<?> type) {
    ResolvedSchema resolvedSchema =
        ModelConverters.getInstance().resolveAsResolvedSchema(new AnnotatedType(type));
    Map<String, Schema> schemas = openApi.getComponents().getSchemas();
    resolvedSchema.referencedSchemas.forEach(
        (name, schema) -> {
          if (schemas != null && schemas.containsKey(name)) {
            return;
          }
          if (schema.getType() == null && schema.getProperties() != null) {
            schema.setType("object");
          }
          openApi.getComponents().addSchemas(name, schema);
        });
  }

  private Operation signInOperation() {
    return new Operation()
        .tags(List.of(AUTH_TAG))
        .summary("로그인")
        .description(SECURITY_FILTER_DESCRIPTION)
        .operationId("signIn")
        .requestBody(
            new RequestBody()
                .required(true)
                .content(
                    new Content()
                        .addMediaType(
                            "application/x-www-form-urlencoded",
                            new MediaType().schema(refSchema("SignInRequest")))))
        .responses(
            new ApiResponses()
                .addApiResponse("200", successResponse("성공", "JwtDto"))
                .addApiResponse("400", errorResponse("잘못된 요청"))
                .addApiResponse("401", errorResponse("인증 오류"))
                .addApiResponse("500", errorResponse("서버 오류")));
  }

  private Operation signOutOperation() {
    return new Operation()
        .tags(List.of(AUTH_TAG))
        .summary("로그아웃")
        .description(SECURITY_FILTER_DESCRIPTION)
        .operationId("signOut")
        .responses(
            new ApiResponses()
                .addApiResponse("204", new ApiResponse().description("성공"))
                .addApiResponse("400", errorResponse("잘못된 요청"))
                .addApiResponse("500", errorResponse("서버 오류")));
  }

  private ApiResponse successResponse(String description, String schemaName) {
    return new ApiResponse()
        .description(description)
        .content(new Content().addMediaType("*/*", new MediaType().schema(refSchema(schemaName))));
  }

  private ApiResponse errorResponse(String description) {
    return new ApiResponse()
        .description(description)
        .content(
            new Content().addMediaType("*/*", new MediaType().schema(refSchema("ErrorResponse"))));
  }

  private Schema<Object> refSchema(String schemaName) {
    return new Schema<>().$ref("#/components/schemas/" + schemaName);
  }
}
