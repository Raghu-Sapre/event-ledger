package org.example.accountservice.web;

import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.example.accountservice.domain.Account;
import org.example.accountservice.service.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping("/apply-event")
    public ResponseEntity<Account> applyEvent(@RequestBody ApplyEventRequest request) {
        Account account = accountService.applyEvent(
                request.accountId(),
                request.amount(),
                request.type()
        );
        return ResponseEntity.ok(account);
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<Account> getAccount(@PathVariable String accountId) {
        return ResponseEntity.ok(accountService.getAccount(accountId));
    }

    public record ApplyEventRequest(String accountId, BigDecimal amount, String type) {}
}
