package org.example.gatewayservice.web;

import org.example.gatewayservice.domain.EventRecord;
import org.example.gatewayservice.repository.EventRecordRepository;
import org.example.gatewayservice.web.dto.EventRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class EventControllerIT {

  @Autowired
  private TestRestTemplate restTemplate;

  @Autowired
  private EventRecordRepository eventRecordRepository;

  @BeforeEach
  void cleanDatabase() {
    eventRecordRepository.deleteAll();
  }

  @Test
  void testFullGatewayEventLifecycle_E2E() {
    // 1. Arrange - Construct an Event Request packet
    Instant earlyTimestamp = Instant.parse("2026-06-01T10:00:00Z");
    EventRequest request = new EventRequest("evt-it-888", "acc-it-user", new BigDecimal("250.50"), "USD", "DEBIT", earlyTimestamp);

    // 2. Act - Process post ingestion
    ResponseEntity<EventRecord> postResponse = restTemplate.postForEntity("/events", request, EventRecord.class);

    // Assert post state
    assertThat(postResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(postResponse.getBody()).isNotNull();
    String primaryKey = postResponse.getBody().getEventId();

    // 3. Act - Validate Retrieval by specific ID (GET /events/{id})
    ResponseEntity<EventRecord> getSingleResponse = restTemplate.getForEntity("/events/" + primaryKey, EventRecord.class);
    assertThat(getSingleResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(getSingleResponse.getBody()).isNotNull();
    assertThat(getSingleResponse.getBody().getAccountId()).isEqualTo("acc-it-user");

    // 4. Act - Validate Retrieval of chronologically ordered event arrays (GET /events?account=X)
    ResponseEntity<List<EventRecord>> listResponse = restTemplate.exchange(
            "/events?account=acc-it-user",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<EventRecord>>() {}
    );

    // Assert query state
    assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    List<EventRecord> records = listResponse.getBody();
    assertThat(records).isNotNull().hasSize(1);
    assertThat(records.get(0).getEventId()).isEqualTo("evt-it-888");
  }
}