package org.example.accountservice.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import org.example.accountservice.domain.Account;
import org.example.accountservice.exception.AccountNotFoundException;
import org.example.accountservice.service.AccountService;
import org.example.accountservice.web.dto.ApplyEventRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AccountController.class)
class AccountControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private AccountService accountService;

  @Test
  void getAccount_WhenExists_ShouldReturn200AndJson() throws Exception {
    Account account = Account.builder().accountId("acc-123").balance(BigDecimal.TEN).build();
    when(accountService.getAccount("acc-123")).thenReturn(account);

    mockMvc
        .perform(get("/accounts/acc-123"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accountId").value("acc-123"))
        .andExpect(jsonPath("$.balance").value(10));
  }

  @Test
  void getAccount_WhenMissing_ShouldReturn404FromHandler() throws Exception {
    when(accountService.getAccount("acc-absent"))
        .thenThrow(new AccountNotFoundException("Account not found with ID: acc-absent"));

    mockMvc
        .perform(get("/accounts/acc-absent"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("Not Found"))
        .andExpect(jsonPath("$.message", containsString("Account not found")));
  }

  @Test
  void applyTransaction_WithValidPayload_ShouldReturn200() throws Exception {
    ApplyEventRequest request =
        new ApplyEventRequest(
            "evt-001", "acc-123", new BigDecimal("50.00"), "CREDIT", Instant.now());
    Account account =
        Account.builder().accountId("acc-123").balance(new BigDecimal("50.00")).build();
    when(accountService.applyEvent(any(ApplyEventRequest.class))).thenReturn(account);

    mockMvc
        .perform(
            post("/accounts/acc-123/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.balance").value(50.00));
  }

  @Test
  void applyTransaction_WithInvalidNegativeAmount_ShouldReturn400BadRequest() throws Exception {
    // Amount -10.00 violates @DecimalMin(value = "0.01")
    ApplyEventRequest request =
        new ApplyEventRequest(
            "evt-001", "acc-123", new BigDecimal("-10.00"), "CREDIT", Instant.now());

    mockMvc
        .perform(
            post("/accounts/acc-123/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(
            jsonPath("$.details[0]", containsString("Amount must be strictly greater than zero")));
  }
}
