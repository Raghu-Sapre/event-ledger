package org.example.gatewayservice.config;

import io.micrometer.tracing.Tracer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

  @Bean
  public WebClient.Builder webClientBuilder() {
    return WebClient.builder();
  }

  @Bean
  public WebClient accountWebClient(
      WebClient.Builder builder,
      @Value("${account-service.base-url:http://localhost:8081}") String baseUrl,
      Tracer tracer) {
    return builder.baseUrl(baseUrl).build();
  }
}
