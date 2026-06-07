package org.example.accountservice.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.accountservice.domain.Account;
import org.example.accountservice.service.AccountService;
import org.example.accountservice.web.dto.ApplyEventRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
@Tag(name = "Accounts", description = "Account balance management APIs")
public class AccountController {

  private final AccountService accountService;

  @GetMapping("/{accountId}")
  @Operation(
          summary = "Get account details",
          description = "Returns the current state of the specified account.",
          responses = {
                  @ApiResponse(
                          responseCode = "200",
                          description = "Account found",
                          content = @Content(schema = @Schema(implementation = Account.class))
                  ),
                  @ApiResponse(responseCode = "404", description = "Account not found")
          }
  )
  public ResponseEntity<Account> getAccount(@PathVariable String accountId) {
    return ResponseEntity.ok(accountService.getAccount(accountId));
  }

  @PostMapping("/apply-event")
  @Operation(
          summary = "Apply an event to an account",
          description = "Applies a credit or debit event to the specified account and returns the updated state.",
          responses = {
                  @ApiResponse(
                          responseCode = "200",
                          description = "Event applied successfully",
                          content = @Content(schema = @Schema(implementation = Account.class))
                  ),
                  @ApiResponse(responseCode = "400", description = "Invalid event"),
                  @ApiResponse(responseCode = "500", description = "Internal server error")
          }
  )
  public ResponseEntity<Account> applyEvent(@RequestBody ApplyEventRequest request) {
    return ResponseEntity.ok(accountService.applyEvent(request));
  }
}
