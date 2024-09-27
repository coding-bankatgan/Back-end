package com.onedrinktoday.backend.global.config;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

  @Bean
  public OperationCustomizer operationCustomizer() {
    return (operation, handlerMethod) -> {
      operation.addParametersItem(
          new Parameter()
              .in(ParameterIn.HEADER.toString())
              .required(false)
              .description("엑세스 토큰")
              .name("Access-Token")
      ).addParametersItem(
          new Parameter()
              .in(ParameterIn.HEADER.toString())
              .required(false)
              .description("리프레시 토큰")
              .name("Refresh-Token")
      );

      return operation;
    };
  }

  @Bean
  public OpenAPI openAPI() {
    return new OpenAPI()
        .components(new Components())
        .info(apiInfo());
  }

  private Info apiInfo() {
    return new Info()
        .title("Drink API")
        .description("swagger")
        .version("1.0.0");
  }
}