package org.example.gatewayservice.service;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.example.gatewayservice.domain.EventRecord;
import org.example.gatewayservice.repository.EventRecordRepository;
import org.example.gatewayservice.web.AccountClient;
import org.example.gatewayservice.web.dto.EventRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EventService {

  private final EventRecordRepository eventRecordRepository;
  private final AccountClient accountClient;

  @Transactional
  public EventRecord processEvent(EventRequest request) {
    // Idempotency: skip if already processed
    Optional<EventRecord> optionalEventRecord =
        eventRecordRepository.findByEventId(request.eventId());
    if (optionalEventRecord.isPresent()) {
      return optionalEventRecord.get();
    }

    // Inside EventService.java
    EventRecord eventRecord =
        EventRecord.builder()
            .eventId(request.eventId())
            .accountId(request.accountId())
            .amount(request.amount())
            .type(request.type())
            .currency(request.currency()) // Save to DB
            .eventTime(
                request.eventTimestamp()) // Use payload timestamp for out-of-order tolerance!
            .build();

    // Call account-service
    accountClient.applyEvent(request);

    return eventRecordRepository.save(eventRecord);
  }

  public Optional<EventRecord> getEventById(String eventId) {
    return eventRecordRepository.findByEventId(eventId);
  }

  public List<EventRecord> getEventsByAccountChronological(String accountId) {
    // Must be explicitly sorted chronologically by eventTimestamp
    return eventRecordRepository.findByAccountIdOrderByEventTimeAsc(accountId);
  }
}
