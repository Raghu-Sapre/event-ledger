package org.example.accountservice.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.example.accountservice.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRepository extends JpaRepository<Account, Long> {

  Optional<Account> findByAccountId(String accountId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT a FROM Account a WHERE a.accountId = :accountId")
  Optional<Account> findByAccountIdWithLock(@Param("accountId") String accountId);
}
