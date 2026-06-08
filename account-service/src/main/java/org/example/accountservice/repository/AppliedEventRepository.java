package org.example.accountservice.repository;

import java.util.Optional;
import org.example.accountservice.domain.AppliedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppliedEventRepository extends JpaRepository<AppliedEvent, Long> {
  Optional<AppliedEvent> findByEventId(String eventId);
}
