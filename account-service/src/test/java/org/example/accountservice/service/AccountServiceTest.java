package org.example.accountservice.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.example.accountservice.domain.Account;
import org.example.accountservice.domain.AccountTransaction;
import org.example.accountservice.exception.AccountNotFoundException;
import org.example.accountservice.repository.AccountRepository;
import org.example.accountservice.repository.AccountTransactionRepository;
import org.example.accountservice.web.dto.ApplyEventRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

  @Mock private AccountRepository accountRepository;

  @Mock private AccountTransactionRepository transactionRepository;

  @InjectMocks private AccountService accountService;

  private String accountId;

  @BeforeEach
  void setUp() {
    accountId = "acc-123";
  }

  @Test
  void getAccount_WhenExists_ShouldReturnAccount() {
    Account expectedAccount =
        Account.builder().accountId(accountId).balance(BigDecimal.TEN).build();
    when(accountRepository.findByAccountId(accountId)).thenReturn(Optional.of(expectedAccount));

    Account actualAccount = accountService.getAccount(accountId);

    assertNotNull(actualAccount);
    assertEquals(accountId, actualAccount.getAccountId());
    assertEquals(BigDecimal.TEN, actualAccount.getBalance());
  }

  @Test
  void getAccount_WhenDoesNotExist_ShouldThrowAccountNotFoundException() {
    when(accountRepository.findByAccountId(accountId)).thenReturn(Optional.empty());

    assertThrows(AccountNotFoundException.class, () -> accountService.getAccount(accountId));
  }

  @Test
  void applyEvent_WhenDuplicateEventId_ShouldReturnIdempotentState() {
    // Arrange
    ApplyEventRequest duplicateRequest =
        new ApplyEventRequest(
            "evt-123", accountId, new BigDecimal("100.00"), "CREDIT", Instant.now());
    Account existingAccount =
        Account.builder().accountId(accountId).balance(new BigDecimal("100.00")).build();

    when(transactionRepository.existsByEventId("evt-123")).thenReturn(true);

    // Stub both variants to cover whichever one your implementation is using under the hood:
    lenient()
        .when(accountRepository.findByAccountId(accountId))
        .thenReturn(Optional.of(existingAccount));
    lenient()
        .when(accountRepository.findByAccountIdWithLock(accountId))
        .thenReturn(Optional.of(existingAccount));

    // Act
    Account result = accountService.applyEvent(duplicateRequest);

    // Assert
    assertNotNull(result);
    assertEquals(new BigDecimal("100.00"), result.getBalance());
  }

  @Test
  void applyEvent_WithOutOfOrderEvents_ShouldRecalculateChronologicalCorrectBalance() {
    // Setup requests: Event 2 physically occurred FIRST but is arriving LATER
    Instant time1 = Instant.parse("2026-06-08T09:00:00Z");
    Instant time2 = Instant.parse("2026-06-08T10:00:00Z");

    ApplyEventRequest lateArrivingEvent =
        new ApplyEventRequest("evt-late", accountId, new BigDecimal("30.00"), "DEBIT", time1);

    Account account =
        Account.builder().accountId(accountId).balance(new BigDecimal("100.00")).build();

    // Mock database transactions timeline representing history already containing the later event
    AccountTransaction existingLaterTx =
        AccountTransaction.builder()
            .eventId("evt-future")
            .accountId(accountId)
            .amount(new BigDecimal("100.00"))
            .type("CREDIT")
            .eventTimestamp(time2)
            .build();

    AccountTransaction newLateTx =
        AccountTransaction.builder()
            .eventId("evt-late")
            .accountId(accountId)
            .amount(new BigDecimal("30.00"))
            .type("DEBIT")
            .eventTimestamp(time1)
            .build();

    when(transactionRepository.existsByEventId("evt-late")).thenReturn(false);

    lenient()
        .when(accountRepository.findByAccountIdWithLock(accountId))
        .thenReturn(Optional.of(account));
    // Critically: The repository returns them sorted chronologically by timestamp (Time1, then
    // Time2)
    List<AccountTransaction> chronologicallySortedList = Arrays.asList(newLateTx, existingLaterTx);
    when(transactionRepository.findByAccountIdOrderByEventTimestampAsc(accountId))
        .thenReturn(chronologicallySortedList);
    when(accountRepository.save(any(Account.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    Account updatedAccount = accountService.applyEvent(lateArrivingEvent);

    // Assert: Net Balance = Sum(Credits) - Sum(Debits) -> 100.00 (Credit) - 30.00 (Debit) = 70.00
    assertNotNull(updatedAccount);
    assertEquals(new BigDecimal("70.00"), updatedAccount.getBalance());

    verify(transactionRepository).save(any(AccountTransaction.class));
    verify(accountRepository).save(any(Account.class));
  }
}
