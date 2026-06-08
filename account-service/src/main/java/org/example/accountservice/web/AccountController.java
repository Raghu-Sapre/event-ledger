package org.example.accountservice.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.accountservice.domain.Account;
import org.example.accountservice.service.AccountService;
import org.example.accountservice.web.dto.ApplyEventRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
@Tag(name = "Accounts", description = "Account balance management APIs")
@Slf4j
public class AccountController {

  private final AccountService accountService;

  @GetMapping("/{accountId}")
  @Operation(
      summary = "Get account details and recent transactions",
      description =
          "Returns the current state of the specified account along with its chronological transaction history.",
      responses = {
        @ApiResponse(responseCode = "200", description = "Account found"),
        @ApiResponse(responseCode = "404", description = "Account not found")
      })
  public ResponseEntity<Account> getAccount(@PathVariable String accountId) {
    log.info("Fetching details for accountId: {}", accountId);
    return ResponseEntity.ok(accountService.getAccount(accountId));
  }

  @GetMapping("/{accountId}/balance")
  @Operation(
      summary = "Get the current balance for an account",
      description = "Returns a simplified object containing just the current net computed balance.",
      responses = {
        @ApiResponse(responseCode = "200", description = "Balance retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Account not found")
      })
  public ResponseEntity<Map<String, Object>> getAccountBalance(@PathVariable String accountId) {
    log.info("Fetching specific balance snapshot for accountId: {}", accountId);
    Account account = accountService.getAccount(accountId);
    return ResponseEntity.ok(
        Map.of(
            "accountId", account.getAccountId(),
            "balance", account.getBalance()));
  }

  @PostMapping("/{accountId}/transactions")
  @Operation(
      summary = "Apply a transaction to an account",
      description =
          "Applies a credit or debit event idempotently and calculates balances chronologically.",
      responses = {
        @ApiResponse(responseCode = "200", description = "Event processed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid event payload constraints"),
        @ApiResponse(responseCode = "404", description = "Account not found target context")
      })
  public ResponseEntity<Account> applyTransaction(
      @PathVariable String accountId, @RequestBody @Valid ApplyEventRequest request) {
    log.info(
        "Processing transaction request for account path: {}, payload account: {}",
        accountId,
        request.accountId());

    // Make the check completely safe against deserialization quirks
    if (request.accountId() != null
        && !accountId.trim().equalsIgnoreCase(request.accountId().trim())) {
      throw new IllegalArgumentException(
          "Account ID in path '"
              + accountId
              + "' does not match request payload target '"
              + request.accountId()
              + "'");
    }

    Account updatedAccount = accountService.applyEvent(request);
    return ResponseEntity.ok(updatedAccount);
  }
}
