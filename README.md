# Distributed Event Ledger Ecosystem

A robust distributed microservice system built with **Spring Boot 3.5.14** and **Java 21** designed to process high-throughput financial transaction logs. The ecosystem is composed of a public edge Event Gateway API and an isolated internal Account Service downstream. It provides deterministic guarantees for transaction idempotency, handles out-of-order event deliveries gracefully, propagates distributed trace contexts, and protects infrastructure components against cascading network failures.

---

## 🏗 1. System Architecture Overview

The ecosystem operates as a distributed system partitioned across two decoupled, independently runnable microservices. To comply with decoupled core data architectures, **neither service shares any database tables, files, or state**:

* **Gateway Service (`port 8080`)**: Acts as the public-facing edge routing ingress. It validates inbound transactions, records a local event footprint into an isolated in-memory audit database (`gatewaydb`), handles trace generation/preservation, and maps requests to the downstream engine via synchronous HTTP calls via a custom pooled `RestClient`.
* **Account Service (`port 8081`)**: Acts as the decoupled downstream financial processing core. It maintains exclusive authority over account balances, coordinates transactional mutations through strict pessimistic write-locking (`LockModeType.PESSIMISTIC_WRITE`) to avoid race conditions, archives a transaction ledger history, and re-computes current net balances dynamically based on the transaction timeline logs.

---

## 🛡️ 2. Resiliency & Fault Isolation Architecture

The synchronous HTTP communication boundary between the `Gateway Service` and the `Account Service` is heavily guarded by a multi-tiered resilience pattern using **Resilience4j** and **Apache HttpClient5 pooling**.

### Coordinated Defense Strategy: Retry + Circuit Breaker
1. **Exponential Backoff Retry (`accountServiceRetry`):** Transient network blips or temporary downstream thread locking are resolved gracefully using a 3-attempt retry loop. It leverages an initial `503ms` delay with a `2x` multiplier and a `0.5` randomized jitter factor to avoid the "thundering herd" problem.
2. **Circuit Breaker (`accountServiceCircuitBreaker`):** If failures persist and cross a 50% error threshold over a rolling sample window, the circuit trips to **OPEN**.
   * **Thread Pool Protection:** This immediately stops the `Gateway Service` from hanging onto Tomcat container threads waiting for a stalled downstream service, completely preventing upstream thread exhaustion.
   * **Downstream Recovery:** The open circuit gives the `Account Service` database connection pool room to breathe and recover from heavy thrashing or locks.
3. **Pooled Connection Management:** Configured via `PoolingHttpClientConnectionManager` with a maximum of 100 total connections and 20 default connections per route, backed by strict 5-second connection and response timeouts.

---

## 📊 3. Core Ledger Engineering Strategies

| System Challenge | Architectural Resolution Strategy | Reference Component |
| :--- | :--- | :--- |
| **Idempotency** | The database layer logs transactions utilizing an atomic `INSERT INTO ... ON CONFLICT (event_id) DO NOTHING` constraint. The raw affected row counts are analyzed; if the count is `0`, the delivery is classified as a duplicate and exits processing without triggering calculations. | `AccountTransactionRepository.insertIfNotExists` |
| **Out-Of-Order Handling** | Balances are never adjusted incrementally via unpredictable incoming events. Instead, the service pulls the *entire historical transaction log* for the target account, forces a strict chronological sorting layer, and recomputes the state from scratch utilizing the absolute ledger formula: <br>$$\text{Net Balance} = \sum(\text{Credits}) - \sum(\text{Debits})$$ | `AccountService.applyEvent` |
| **Distributed Tracing** | Trace context is automatically intercepted and injected across service boundaries via standard **W3C Trace Context Headers** (`traceparent`). Incoming trace structures are preserved to ensure unified distributed trace identification. | `Gateway Client` $\rightarrow$ `Account Controller` |

---

## 🛠 4. Prerequisites & Setup

Ensure the following environments are configured on your workstation before starting the stack:
* **Docker Engine** paired with **Docker Desktop** (configured with a minimum of 4GB allocated memory).
* **Java 21 LTS** installation.
* **Apache Maven 3.9+** mapped into system environment variables.

---

## 🚀 5. Step-by-Step Launch Sequence

### Step 1: Compile and Package Binaries
Build the multi-module artifact jars from the root directory while running standard static code checks and formatting verification loops:
```bash
mvn clean package