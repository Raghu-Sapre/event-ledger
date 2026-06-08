package org.example.accountservice.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.*;

@Entity // CRITICAL: Tells Hibernate to generate the 'account_transactions' table automatically
@Table(
    name = "account_transactions",
    indexes = {@Index(name = "idx_event_id", columnList = "eventId", unique = true)})
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
