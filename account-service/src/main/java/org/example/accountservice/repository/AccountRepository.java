package org.example.accountservice.repository;

import java.util.Optional;
import org.example.accountservice.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {

  Optional<Account> findByAccountId(String accountId);
}
