package org.example.accountservice.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String accountId;

  @Column(nullable = false)
  private BigDecimal balance;
}
