package org.example.accountservice;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import org.example.accountservice.domain.Account;
import org.example.accountservice.repository.AccountRepository;
import org.example.accountservice.repository.AccountTransactionRepository;
import org.example.accountservice.web.dto.ApplyEventRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ActiveProfiles("test")
class AccountServiceApplicationIT {

  @Autowired private TestRestTemplate restTemplate;

  @Autowired private AccountRepository accountRepository;

  @Autowired private AccountTransactionRepository transactionRepository;

  @BeforeEach
  void cleanDatabase() {
    transactionRepository.deleteAll();
    accountRepository.deleteAll();
  }

  @Test
  void fullLedgerE2EFlow_ShouldProcessOutOrOrderAndIdempotentRequestsCorrectly() {
    String accountId = "acc-integration-test";
    Instant baseTime = Instant.parse("2026-06-08T12:00:00Z");

    // 1. Submit a first event (Credit $200.00 at 12:00 PM)
    ApplyEventRequest event1 =
        new ApplyEventRequest("e1", accountId, new BigDecimal("200.00"), "CREDIT", baseTime);
    ResponseEntity<Account> response1 =
        restTemplate.postForEntity(
            "/accounts/" + accountId + "/transactions", event1, Account.class);
    assertEquals(HttpStatus.OK, response1.getStatusCode());
    assertEquals(new BigDecimal("200.00"), response1.getBody().getBalance());

    // 2. Submit a duplicate of event 1 (Idempotency testing)
    ResponseEntity<Account> responseDuplicate =
        restTemplate.postForEntity(
            "/accounts/" + accountId + "/transactions", event1, Account.class);
    assertEquals(HttpStatus.OK, responseDuplicate.getStatusCode());
    assertEquals(new BigDecimal("200.00"), responseDuplicate.getBody().getBalance());
    assertEquals(
        1, transactionRepository.count(), "Should not create a duplicate transaction record");

    // 3. Submit a second event that happened in the FUTURE (Debit $50.00 at 13:00 PM)
    ApplyEventRequest event3Future =
        new ApplyEventRequest(
            "e3", accountId, new BigDecimal("50.00"), "DEBIT", baseTime.plusSeconds(3600));
    restTemplate.postForEntity(
        "/accounts/" + accountId + "/transactions", event3Future, Account.class);

    // 4. Submit an OUT-OF-ORDER event that happened in the PAST (Credit $10.00 at 11:00 AM)
    ApplyEventRequest event2Past =
        new ApplyEventRequest(
            "e2", accountId, new BigDecimal("10.00"), "CREDIT", baseTime.minusSeconds(3600));
    ResponseEntity<Account> finalResponse =
        restTemplate.postForEntity(
            "/accounts/" + accountId + "/transactions", event2Past, Account.class);

    // Verify Balance Computation Formula: Net Balance = Sum(Credits) - Sum(Debits)
    // Order of Arrival: e1 ($200 C), e3 ($50 D), e2 ($10 C)
    // Order of Occurrence: e2 ($10 C) -> e1 ($200 C) -> e3 ($50 D)
    // Calculation: 10.00 + 200.00 - 50.00 = 160.00
    assertEquals(HttpStatus.OK, finalResponse.getStatusCode());
    assertEquals(new BigDecimal("160.00"), finalResponse.getBody().getBalance());

    // 5. Query the balance snapshot endpoint to verify isolation contract compliance
    ResponseEntity<Map> balanceResponse =
        restTemplate.getForEntity("/accounts/" + accountId + "/balance", Map.class);
    assertEquals(HttpStatus.OK, balanceResponse.getStatusCode());
    assertEquals(160.0, balanceResponse.getBody().get("balance"));
  }


}
