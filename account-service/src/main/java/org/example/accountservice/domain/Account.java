package org.example.accountservice.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;

@Entity
@Table(
    name = "accounts",
    indexes = {@Index(name = "idx_account_id", columnList = "accountId")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Represents an account with a running net balance")
public class Account {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Schema(description = "Internal database identifier", example = "1")
  private Long id;

  @Column(unique = true, nullable = false)
  @Schema(description = "Business account identifier used in API routes", example = "acc-001")
  private String accountId;

  // Added nullable = false constraint to prevent DB null corruptions
  @Column(nullable = false)
  @Schema(description = "Current net balance calculated post-sort", example = "500.00")
  private BigDecimal balance;

  public Account(String accountId, BigDecimal balance) {
    this.accountId = accountId;
    this.balance = balance;
  }
}
