package org.example.gatewayservice.web;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gatewayservice.web.dto.EventRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountClient {

  private final WebClient accountWebClient;

  @CircuitBreaker(name = "accountService", fallbackMethod = "fallback")
  public void applyEvent(EventRequest request) {
    accountWebClient
        .post()
        .uri("/accounts/apply-event")
        .bodyValue(request)
        .retrieve()
        .toBodilessEntity()
        .block();
  }

  @SuppressWarnings("unused")
  private void fallback(EventRequest request, Throwable ex) {
    // For now, just log or ignore; you can add DLQ / retry later
    log.info("Circuit breaker opened for account-service: {}", ex.getMessage());
  }
}
