package org.example.gatewayservice.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.example.gatewayservice.domain.EventRecord;
import org.example.gatewayservice.repository.EventRecordRepository;
import org.example.gatewayservice.web.dto.EventRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class EventControllerIT {

  @Autowired private TestRestTemplate restTemplate;

  @Autowired private EventRecordRepository eventRecordRepository;

  @BeforeEach
  void cleanDatabase() {
    eventRecordRepository.deleteAll();
  }

  @Test
  void ingestEvent_FullFlowIntegrationTest() {
    // Given
    EventRequest requestRecord =
        new EventRequest("evt-it-101", "acc-integration", new BigDecimal("250.50"), "DEBIT");

    // When
    ResponseEntity<EventRecord> response =
        restTemplate.postForEntity("/events", requestRecord, EventRecord.class);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getEventId()).isEqualTo("evt-it-101");
    assertThat(response.getBody().getAccountId()).isEqualTo("acc-integration");

    // Verify the database recorded the entry
    long databaseCount = eventRecordRepository.count();
    assertThat(databaseCount).isEqualTo(1);
  }
}
