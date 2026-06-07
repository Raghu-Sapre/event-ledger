package org.example.gatewayservice.repository;

import java.util.Optional;
import org.example.gatewayservice.domain.EventRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRecordRepository extends JpaRepository<EventRecord, Long> {

  Optional<EventRecord> findByEventId(String eventId);
}
