package org.example.gatewayservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.util.Optional;
import org.example.gatewayservice.domain.EventRecord;
import org.example.gatewayservice.repository.EventRecordRepository;
import org.example.gatewayservice.web.AccountClient;
import org.example.gatewayservice.web.dto.EventRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test") // Ensures your in-memory configuration handles the database state
class EventServiceIT {

  @Autowired private EventService eventService;

  @Autowired private EventRecordRepository eventRecordRepository;

  // Mock the external network client to isolate the Gateway's local integration test
  @MockitoBean private AccountClient accountClient;

  @BeforeEach
  void setUp() {
    eventRecordRepository.deleteAll();
  }

  @Test
  void processEvent_FullDatabaseIntegrationAndIdempotencyFlow() {
    // Given
    EventRequest firstRequest =
        new EventRequest("evt-it-555", "acc-it", new BigDecimal("50.00"), "DEBIT");

    // When - First execution (New Event)
    EventRecord savedRecord = eventService.processEvent(firstRequest);

    // Then - Validate real database state persistence
    assertThat(savedRecord.getId()).isNotNull(); // Proves it hit the real H2 auto-increment logic
    Optional<EventRecord> dbRecord = eventRecordRepository.findByEventId("evt-it-555");
    assertThat(dbRecord).isPresent();
    assertThat(dbRecord.get().getAccountId()).isEqualTo("acc-it");
    verify(accountClient, times(1)).applyEvent(firstRequest);

    // When - Second execution (Duplicate Event with same EventID)
    EventRecord duplicatedResult = eventService.processEvent(firstRequest);

    // Then - Confirm local DB was not modified and idempotency held true
    assertThat(duplicatedResult.getId()).isEqualTo(savedRecord.getId());
    assertThat(eventRecordRepository.count()).isEqualTo(1); // Row count remains exactly 1
    verify(accountClient, times(1))
        .applyEvent(firstRequest); // The call count did not increase to 2
  }
}
