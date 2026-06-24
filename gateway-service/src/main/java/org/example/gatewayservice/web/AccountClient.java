package org.example.gatewayservice.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gatewayservice.web.dto.EventRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountClient {

  private final RestClient accountRestClient;

  public void applyEvent(EventRequest request) {
    log.debug(
        "Sending synchronous POST request to account-service for event: {}", request.eventId());

    accountRestClient
        .post()
        .uri("/accounts/" + request.accountId() + "/transactions")
        .body(request)
        .retrieve()
        .toBodilessEntity(); // Automatically throws exceptions on 4xx or 5xx responses
  }
}
