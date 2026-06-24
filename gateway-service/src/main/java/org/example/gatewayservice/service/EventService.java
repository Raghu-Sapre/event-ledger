package org.example.gatewayservice.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gatewayservice.domain.EventRecord;
import org.example.gatewayservice.repository.EventRecordRepository;
import org.example.gatewayservice.web.AccountClient;
import org.example.gatewayservice.web.dto.EventRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

  private final EventRecordRepository eventRecordRepository;
  private final AccountClient accountClient;

  /**
   * Orchestrates incoming events. Tries to pass them downstream synchronously, but safely drops
   * into an asynchronous-reconciliation path if an outage occurs.
   */
  @Transactional
  // 1. Attempts up to 3 times with exponential random jitter delays matching application.yml
  @Retry(name = "accountService")
  // 2. Trips open to protect the ecosystem if failure thresholds exceed 50%
  @CircuitBreaker(name = "accountService", fallbackMethod = "handleAccountServiceFailure")
  public EventRecord processEvent(EventRequest request) {

    // Idempotency check: skip processing if already captured in our gateway ledger
    Optional<EventRecord> optionalEventRecord =
        eventRecordRepository.findByEventId(request.eventId());
    if (optionalEventRecord.isPresent()) {
      log.info(
          "Event {} already fully captured by Gateway. Skipping downstream execution.",
          request.eventId());
      return optionalEventRecord.get();
    }

    EventRecord eventRecord = buildRecord(request);

    log.info("Forwarding event {} synchronously to downstream account-service.", request.eventId());
    accountClient.applyEvent(request);

    return eventRecordRepository.save(eventRecord);
  }

  /**
   * CRITICAL RESILIENCY FALLBACK METHOD Executed automatically when downstream retries exhaust, or
   * when the Circuit Breaker is OPEN.
   */
  @Transactional
  public EventRecord handleAccountServiceFailure(EventRequest request, Throwable exception) {
    log.error(
        "CRITICAL: Downstream tracking failed for event {}. Circuit Breaker status handled. Reason: {}. Saving locally for asynchronous recovery.",
        request.eventId(),
        exception.getMessage());

    // Idempotency check for the fallback path to prevent duplicate database rows
    Optional<EventRecord> optionalEventRecord =
        eventRecordRepository.findByEventId(request.eventId());
    if (optionalEventRecord.isPresent()) {
      return optionalEventRecord.get();
    }

    EventRecord offlineRecord = buildRecord(request);

    // The event is saved safely to our local database for asynchronous reconciliation later
    return eventRecordRepository.save(offlineRecord);
  }

  public Optional<EventRecord> getEventById(String eventId) {
    return eventRecordRepository.findByEventId(eventId);
  }

  public List<EventRecord> getEventsByAccountChronological(String accountId) {
    return eventRecordRepository.findByAccountIdOrderByEventTimeAsc(accountId);
  }

  private EventRecord buildRecord(EventRequest request) {
    return EventRecord.builder()
        .eventId(request.eventId())
        .accountId(request.accountId())
        .amount(request.amount())
        .type(request.type())
        .currency(request.currency())
        .eventTime(
            request.eventTimestamp()) // Preserves original timestamp for out-of-order tolerance
        .build();
  }
}
