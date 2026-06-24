package org.example.accountservice.repository;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.List;

import org.example.accountservice.domain.AccountTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountTransactionRepository extends JpaRepository<AccountTransaction, Long> {

  // Atomic idempotency: If the event_id exists, do nothing and return 0
  @Modifying
  @Query(value = "INSERT INTO account_transactions (event_id, account_id, amount, event_timestamp) " +
          "VALUES (:eventId, :accountId, :amount, :timestamp) " +
          "ON CONFLICT (event_id) DO NOTHING",
          nativeQuery = true)
  int insertIfNotExists(@Param("eventId") String eventId,
                        @Param("accountId") String accountId,
                        @Param("amount") BigDecimal amount,
                        @Param("timestamp") Instant timestamp);

  List<AccountTransaction> findByAccountIdOrderByEventTimestampAsc(String accountId);
}