package org.example.accountservice.repository;

import org.example.accountservice.domain.AppliedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppliedEventRepository extends JpaRepository<AppliedEvent, Long> {
  Optional<AppliedEvent> findByEventId(String eventId);
}
