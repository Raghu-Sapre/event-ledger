package org.example.accountservice.service;

import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.accountservice.domain.Account;
import org.example.accountservice.domain.AccountTransaction;
import org.example.accountservice.exception.AccountNotFoundException;
import org.example.accountservice.repository.AccountRepository;
import org.example.accountservice.repository.AccountTransactionRepository;
import org.example.accountservice.web.dto.ApplyEventRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

  private final AccountRepository accountRepository;
  private final AccountTransactionRepository transactionRepository;

  /**
   * Fetches an account by its unique business ID. If the account does not exist, it throws a
   * localized exception or handles safely.
   */
  public Account getAccount(String accountId) {
    return accountRepository
        .findByAccountId(accountId)
        .orElseThrow(() -> new AccountNotFoundException("Account not found with ID: " + accountId));
  }

  /** Applies an event to an account safely handling out-of-order and duplicate deliveries. */
  @Transactional
  public Account applyEvent(ApplyEventRequest request) {
    log.info("Applying event {} to account {}", request.eventId(), request.accountId());

    // 1. Idempotency Check: If eventId already processed, bypass calculation completely
    if (transactionRepository.existsByEventId(request.eventId())) {
      log.warn(
          "Duplicate event detected! Event ID: {} has already been applied. Returning current state idempotently.",
          request.eventId());
      return accountRepository
          .findByAccountId(request.accountId())
          .orElseGet(() -> createNewAccount(request.accountId(), BigDecimal.ZERO));
    }

    // 2. Fetch account or initialize a new one if it's their very first transaction
    Account account =
        accountRepository
            .findByAccountId(request.accountId())
            .orElseGet(() -> createNewAccount(request.accountId(), BigDecimal.ZERO));

    // 3. Save the historical transaction record
    AccountTransaction newTx =
        AccountTransaction.builder()
            .eventId(request.eventId())
            .accountId(request.accountId())
            .amount(request.amount())
            .type(request.type().toUpperCase())
            .eventTimestamp(request.eventTimestamp()) // Injected timestamp from your updated DTO
            .build();
    transactionRepository.save(newTx);

    // 4. Resolve Out-of-Order Execution: Pull ALL history sorted chronologically by timestamp
    List<AccountTransaction> sortedHistory =
        transactionRepository.findByAccountIdOrderByEventTimestampAsc(request.accountId());

    // 5. Re-compute Net Balance strictly using the formula: Sum(Credits) - Sum(Debits)
    BigDecimal calculatedBalance = BigDecimal.ZERO;
    for (AccountTransaction tx : sortedHistory) {
      if ("CREDIT".equals(tx.getType())) {
        calculatedBalance = calculatedBalance.add(tx.getAmount());
      } else if ("DEBIT".equals(tx.getType())) {
        calculatedBalance = calculatedBalance.subtract(tx.getAmount());
      }
    }

    // 6. Update cached account entity balance and save changes
    account.setBalance(calculatedBalance);
    return accountRepository.save(account);
  }

  private Account createNewAccount(String accountId, BigDecimal initialBalance) {
    log.info(
        "First-time event interaction for account {}. Creating record with initial balance {}.",
        accountId,
        initialBalance);
    Account newAccount = Account.builder().accountId(accountId).balance(initialBalance).build();
    return accountRepository.save(newAccount);
  }
}
