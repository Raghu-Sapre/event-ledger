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
```
How to Run the Infrastructure
Prerequisites
Make sure you have Docker and Docker Compose installed on your system, and that the Docker daemon is actively running.

1. Compile the Project Assets
Before running the containers, build the native executable jar artifacts using the Maven wrapper:
# From the project root directory
```bash
mvn clean package -DskipTests
```
Launch the Stack
Spin up the microservices along with the full telemetry pipeline in detached mode:
```bash
docker compose up --build -d
```
The Gateway Service will wait to fully start until the Account Service passes its automated health check.

3. Terminate the Stack
To stop the application network and clear active container configurations without losing data volumes:
```bash
docker compose down
```

**Accessing Dashboards & Documentation**

Once the infrastructure is up, you can access the service entry points and diagnostic dashboards using the following URLs:

🛠️ **Service Documentation & Interacting**
Gateway Service Swagger UI: http://localhost:8080/swagger-ui/index.htmlAccount 

Service Swagger UI: http://localhost:8081/swagger-ui/index.html

Use these to fire test transactional payloads into the endpoints and generate live telemetry data.
🔍 **Observability Systems**
**SystemURL TargetCore Diagnostic Capability

Jaeger UI   http://localhost:16686

**End-to-end distributed transaction tracing**

Prometheus http://localhost:9090 Raw time-series performance metrics scraping
Grafana  http://localhost:3000 High-fidelity dashboard visualization layer


📈 How to Inspect Traces, Metrics, and Logs1. 

1. Tracking Distributed Traces (Jaeger)
Distributed tracing follows a request as it travels from the Gateway Service to the Account Service.
Open the Jaeger UI (http://localhost:16686).
Under Service, select gateway-service.
Click Find Traces.
Click into any trace block to see exactly how long the request spent inside the Gateway vs. the downstream Account Service processing logic.

2. Inspecting Performance Metrics (Prometheus & Grafana) Metrics track system performance, such as request counts, error rates, and JVM memory.
3. Raw Metrics Data: You can view the real-time Prometheus string exposure lines directly via the actuator scrapers at http://localhost:8080/actuator/prometheus or http://localhost:8081/actuator/prometheus. 
4. Visual Dashboards (Grafana):Open Grafana (http://localhost:3000) and log in (Default: admin / admin).
5. Go to Connections -> Data Sources and add Prometheus with the URL http://prometheus:9090. 
6. Click Save & Test.Go to Dashboards -> Import, enter ID 4701 (JVM Micrometer Core) or 11378 (Spring Boot Dashboard), and click Load to visualize real-time application behavior graphs.
7. Checking Structured JSON Logs (Docker) Both microservices use structured Logstash JSON layout formatters that output straight to standard console pipes.
8. To stream live logs from the cluster and watch transaction events or exceptions parse in real-time, execute:Bash

# Stream logs from all services in the cluster
```bash
docker compose logs -f
```
# Stream logs exclusively from a single service
```bash
docker compose logs -f gateway-service
docker compose logs -f account-service
```