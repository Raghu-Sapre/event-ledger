package org.example.gatewayservice.service;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

import java.math.BigDecimal;
import java.time.Instant;
import org.example.gatewayservice.web.dto.EventRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = "account-service.base-url=http://localhost:8081")
@AutoConfigureWireMock(port = 8081)
@ActiveProfiles("test")
class EventServiceResiliencyIT {

  @Autowired private EventService eventService;

  @Test
  void shouldRetryAndSucceed_WhenAccountServiceFlaky() {
    // Arrange: Simulate 2 failures followed by 1 success

    stubFor(
        post(urlPathMatching("/accounts/.*/transactions"))
            .willReturn(aResponse().withStatus(503)) // Fail
            .inScenario("AccountServiceScenario") // 1. Define the scenario name
            .willSetStateTo("FirstAttempt")); // 2. Set the state name here

    stubFor(
        post(urlPathMatching("/accounts/.*/transactions"))
            .inScenario("AccountServiceScenario")
            .whenScenarioStateIs("FirstAttempt")
            .willReturn(aResponse().withStatus(503)) // Fail again
            .willSetStateTo("SecondAttempt")); // Use willSetStateTo

    stubFor(
        post(urlPathMatching("/accounts/.*/transactions"))
            .inScenario("AccountServiceScenario")
            .whenScenarioStateIs("SecondAttempt")
            .willReturn(aResponse().withStatus(200))); // Finally succeed
    // Act
    EventRequest request =
        new EventRequest(
            "evt-123", "acc-001", new BigDecimal("100.00"), "USD", "CREDIT", Instant.now());
    eventService.processEvent(request);

    // Assert: Verify wiremock received 3 calls
    verify(3, postRequestedFor(urlPathMatching("/accounts/.*/transactions")));
  }
}
