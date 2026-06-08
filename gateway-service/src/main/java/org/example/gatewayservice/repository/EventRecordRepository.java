package org.example.gatewayservice.repository;

import java.util.List;
import java.util.Optional;
import org.example.gatewayservice.domain.EventRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRecordRepository extends JpaRepository<EventRecord, Long> {

  Optional<EventRecord> findByEventId(String eventId);

  /**
   * Fetches all events associated with a specific account ID,
   * automatically sorted in chronological order (Oldest to Newest).
   */
  List<EventRecord> findByAccountIdOrderByEventTimeAsc(String accountId);
}