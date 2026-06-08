package org.example.gatewayservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

  /**
   * Exposes a default WebClient.Builder or WebClient instance into the Spring context so
   * AccountClient can inject it cleanly.
   */
  @Bean
  public WebClient webClient(WebClient.Builder builder) {
    // You can also pre-configure global base URLs here if desired
    return builder.build();
  }
}
