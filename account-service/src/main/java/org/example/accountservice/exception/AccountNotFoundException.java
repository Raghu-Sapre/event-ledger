package org.example.accountservice.exception;

/**
 * Custom runtime exception thrown when an requested account identifier cannot be found in the
 * database. Maps to a 404 HTTP Status.
 */
public class AccountNotFoundException extends RuntimeException {

  public AccountNotFoundException(String message) {
    super(message);
  }
}
