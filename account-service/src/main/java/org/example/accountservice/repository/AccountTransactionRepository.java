package org.example.accountservice.repository;

import java.util.List;
import org.example.accountservice.domain.AccountTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountTransactionRepository extends JpaRepository<AccountTransaction, Long> {

  // Used to immediately enforce Idempotency (returns true if event was already processed)
  boolean existsByEventId(String eventId);

  // CRITICAL: Fetches history pre-sorted by timestamp to execute the mandatory calculation formula
  List<AccountTransaction> findByAccountIdOrderByEventTimestampAsc(String accountId);
}
