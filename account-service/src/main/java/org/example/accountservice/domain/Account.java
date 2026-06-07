package org.example.accountservice.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Represents an account with a running balance")
public class Account {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Schema(description = "Database identifier", example = "1")
  private Long id;

  @Schema(description = "Account identifier", example = "acc-001")
  private String accountId;

  @Schema(description = "Current balance", example = "500.00")
  private BigDecimal balance;
}
