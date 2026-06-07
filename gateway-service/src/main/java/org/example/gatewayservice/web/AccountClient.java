package org.example.gatewayservice.web;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.example.gatewayservice.web.dto.EventRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class AccountClient {

  private final WebClient accountWebClient;

  @CircuitBreaker(name = "accountService", fallbackMethod = "fallbackApplyEvent")
  public void applyEvent(EventRequest request) {
    accountWebClient
        .post()
        .uri("/accounts/apply-event")
        .bodyValue(request)
        .retrieve()
        .toBodilessEntity()
        .block();
  }

  // Fallback signature must match + Throwable at end
  private void fallbackApplyEvent(EventRequest request, Throwable ex) {
    // log, enqueue for retry, etc.
  }
}
