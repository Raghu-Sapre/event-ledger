package org.example.accountservice.service;

import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.example.accountservice.domain.Account;
import org.example.accountservice.repository.AccountRepository;
import org.example.accountservice.web.dto.ApplyEventRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountService {

  private final AccountRepository accountRepository;

  @Transactional
  public Account applyEvent(ApplyEventRequest request) {

    String accountId = request.accountId();
    BigDecimal amount = request.amount();
    String type = request.type();

    Account account =
        accountRepository
            .findByAccountId(accountId)
            .orElseGet(
                () -> Account.builder().accountId(accountId).balance(BigDecimal.ZERO).build());

    BigDecimal newBalance =
        switch (type) {
          case "CREDIT" -> account.getBalance().add(amount);
          case "DEBIT" -> account.getBalance().subtract(amount);
          default -> throw new IllegalArgumentException("Unknown type: " + type);
        };

    account.setBalance(newBalance);
    return accountRepository.save(account);
  }

  @Transactional(readOnly = true)
  public Account getAccount(String accountId) {
    return accountRepository
        .findByAccountId(accountId)
        .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
  }
}
