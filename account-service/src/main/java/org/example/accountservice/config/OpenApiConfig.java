package org.example.accountservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI accountOpenAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Account Service API")
                .description("Manages account balances and applies events")
                .version("1.0.0"));
  }
}
