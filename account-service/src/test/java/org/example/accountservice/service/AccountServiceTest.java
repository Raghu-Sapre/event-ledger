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

    // 1. Stub atomic insert as duplicate
    when(transactionRepository.insertIfNotExists(
            anyString(), anyString(), any(BigDecimal.class), any(Instant.class)))
        .thenReturn(0);

    Account existingAccount =
        Account.builder().accountId(accountId).balance(new BigDecimal("100.00")).build();

    // Stub for the standard lookup (getAccount() used in the service)
    when(accountRepository.findByAccountId(accountId)).thenReturn(Optional.of(existingAccount));

    // Act
    Account result = accountService.applyEvent(duplicateRequest);

    // Assert
    assertNotNull(result);
    assertEquals(new BigDecimal("100.00"), result.getBalance());
    verify(transactionRepository, never()).findByAccountIdOrderByEventTimestampAsc(anyString());
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

    // Replace the old existsByEventId stub with the new insertIfNotExists stub
    // Return 1 to simulate a successful (non-duplicate) insert
    when(transactionRepository.insertIfNotExists(
            anyString(), anyString(), any(BigDecimal.class), any(Instant.class)))
        .thenReturn(1);

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

  @Test
  void applyEvent_ShouldCorrectlyRecomputeBalance_WhenEventsArriveOutOfOrder() {
    // 1. Arrange: Setup an account and transactions
    String accId = "acc-001";
    Instant t1 = Instant.parse("2026-06-08T09:00:00Z");
    Instant t2 = Instant.parse("2026-06-08T10:00:00Z");

    // Account state representing the account after it is created/saved
    Account account = Account.builder().accountId(accId).balance(BigDecimal.ZERO).build();

    AccountTransaction txA =
        AccountTransaction.builder()
            .eventId("evt-A")
            .accountId(accId)
            .amount(new BigDecimal("100.00"))
            .type("CREDIT")
            .eventTimestamp(t1)
            .build();
    AccountTransaction txB =
        AccountTransaction.builder()
            .eventId("evt-B")
            .accountId(accId)
            .amount(new BigDecimal("50.00"))
            .type("CREDIT")
            .eventTimestamp(t2)
            .build();

    // Mock atomic insertion returning 1 (success) for both calls
    when(transactionRepository.insertIfNotExists(
            anyString(), anyString(), any(BigDecimal.class), any(Instant.class)))
        .thenReturn(1);

    // Track the account retrieval: First call doesn't find it (triggers createNewAccount),
    // Second call finds the existing account instance.
    when(accountRepository.findByAccountIdWithLock(accId))
        .thenReturn(Optional.empty()) // First call
        .thenReturn(Optional.of(account)); // Second call

    // Mock the stateful return of history query sequentially
    when(transactionRepository.findByAccountIdOrderByEventTimestampAsc(accId))
        .thenReturn(List.of(txB)) // After Event B is added (chronologically just B)
        .thenReturn(List.of(txA, txB)); // After Event A is added (chronologically A then B)

    // Capture saved objects cleanly
    when(accountRepository.save(any(Account.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(transactionRepository.save(any(AccountTransaction.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // 2. Act: Insert Event B (t2) first, then Event A (t1)
    accountService.applyEvent(buildRequest("evt-B", accId, 50, t2));
    Account finalAccount = accountService.applyEvent(buildRequest("evt-A", accId, 100, t1));

    // 3. Assert: 100.00 + 50.00 = 150.00
    assertNotNull(finalAccount);
    assertEquals(new BigDecimal("150.00"), finalAccount.getBalance());
  }

  // Missing helper method to build the ApplyEventRequest
  private ApplyEventRequest buildRequest(
      String eventId, String accountId, double amount, Instant timestamp) {
    return new ApplyEventRequest(
        eventId, accountId, BigDecimal.valueOf(amount).setScale(2), "CREDIT", timestamp);
  }
}
