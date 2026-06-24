package org.example.accountservice.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.*;

@Entity
@Table(
    name = "account_transactions",
    indexes = {
      @Index(name = "idx_event_id", columnList = "eventId", unique = true),
      // CRITICAL PERFORMANCE INDEX: Accelerates out-of-order chronological calculation queries
      @Index(name = "idx_account_timestamp", columnList = "accountId, eventTimestamp")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountTransaction {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String eventId;

  @Column(nullable = false)
  private String accountId;

  @Column(nullable = false)
  private BigDecimal amount;

  @Column(nullable = false)
  private String type; // CREDIT or DEBIT

  @Column(nullable = false)
  private Instant eventTimestamp;
}
