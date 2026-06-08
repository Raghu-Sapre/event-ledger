This document serves as your official compliance matrix for the Event Ledger assignment. 
It maps each mandatory requirement from the project prompt to the specific engineering decisions and implementations we have completed.📋 Compliance MatrixRequirementImplementation DetailStatusIdempotencyDatabase unique constraint on eventId + Service-level findByEventId check.✅ Met

Out-of-Order ToleranceRepository uses findByAccountIdOrderByEventTimeAsc based on the payload's eventTimestamp.✅ Met

Balance ComputationStandard logic: $\sum \text{CREDITS} - \sum \text{DEBITS}$.✅ Met

Service SeparationTwo independent services; no shared database/state; isolated H2 in-memory stores.✅ Met

Distributed TracingMicrometer Tracing enabled; Trace propagation via headers; logged via structured JSON.✅ Met

Structured Logginglogstash-logback-encoder used to output JSON with trace/span IDs and service metadata.✅ Met

Health Checks/health endpoints implemented on both services for container lifecycle management.✅ Met

ResiliencyResilience4j Circuit Breaker implemented on the AccountClient in the Gateway.✅ Met

Graceful DegradationCircuit breaker prevents hangs; Gateway returns 503 for writes, allows reads to continue.✅ Met

Docker ComposeUnified docker-compose.yml with Jaeger, Prometheus, and Grafana integration.✅ Met

Automated TestingUnit tests (*Test.java) and Integration tests (*IT.java) for all core/resiliency paths.✅ Met


🏗️ Architectural OverviewThe system is designed as a Synchronous REST Microservices Architecture. 
The Event Gateway serves as the public-facing ingestion point , responsible for input validation, idempotency enforcement, and initial event persistence.
It acts as a resilient buffer, ensuring that the Account Service (the internal engine for balance state) is only invoked for necessary transactional mutations.  

🛠️ Requirement Implementation Details1. 
Resiliency Strategy: Circuit BreakerWe implemented the Circuit Breaker pattern. 
When the Account Service experiences sustained failure, the Gateway trips the circuit, immediately returning a 503 Service Unavailable response to the client for write operations. 
This protects the Gateway from thread pool exhaustion.  

3. Trace PropagationEvery incoming request at the Gateway is assigned a traceId via Micrometer Tracing. 
This ID is injected into outgoing HTTP headers and passed to the Account Service. 
Both services utilize a logback-spring.xml configuration with the Logstash Encoder to ensure that all logs are emitted as structured JSON objects, capturing the traceId, spanId, service, and level fields automatically.

5. Idempotency & Chronological OrderThe Gateway handles the "out-of-order" requirement by storing the client-provided eventTimestamp. Queries for account history use a JPA-derived repository method (findByAccountIdOrderByEventTimeAsc) to ensure the response is always returned in the correct chronological order, regardless of the arrival time. 
Idempotency is enforced by verifying the existence of the eventId in the database prior to processing.  

How to Verify ComplianceTests: Run mvn clean verify to trigger the full integration test suite, which confirms idempotency, validation logic, and successful H2 persistence loops. 

7. Observability: Launch the Docker stack and navigate to http://localhost:16686 (Jaeger) to see the full trace propagation path.  
8. Metrics: Navigate to http://localhost:3000 (Grafana) to view the custom metrics emitted by the application.  