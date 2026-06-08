# event-ledger
event-ledger
# Event Ledger Ecosystem

A modern distributed microservice architecture utilizing Spring Boot 3.5.1, Java 25, and high-fidelity observability tracking. This platform processes transaction ledgers through an API Gateway, backed by containerized in-memory H2 databases and full telemetry infrastructure.

## 🏗 Architecture Overview

The system consists of the following components running in a unified Docker network:
* **Gateway Service (`port 8080`)**: Ingress edge router protected by Resilience4j Circuit Breakers.
* **Account Service (`port 8081`)**: Downstream ledger processor managing transactional accounts.
* **Jaeger (`port 16686`)**: Distributed tracing visualization UI (OTLP HTTP collector).
* **Prometheus (`port 9090`)**: Time-series database scraping micrometer telemetry variables.
* **Grafana (`port 3000`)**: Professional visualization layer for performance metrics.

---

## 🛠 Prerequisites for Windows

Before running the stack, ensure you have the following installed on your machine:
1. **Windows 10/11** with [Docker Desktop](https://www.docker.com/products/docker-desktop/) installed and running (using WSL 2 backend).
2. **Java 21 or 25** configured locally.
3. **Apache Maven 3.9+** configured in your system environment variables.

---

## 🚀 Step-by-Step Launch Sequence

Open your **PowerShell** or **Command Prompt** terminal at the project root (`C:\Users\raghu\IdeaProjects\event-ledger`) and run the following commands in order:

### 1. Compile the Multi-Module Binaries
Compile and package the fresh `.jar` files for both microservices concurrently:
```bash
mvn clean package -DskipTests

2. Boot Up the Infrastructure Stack
Deploy the full container cluster in detached (background) mode:

docker compose up --build -d

PS C:\Users\raghu\IdeaProjects\event-ledger> docker compose up --build -d


time="2026-06-08T02:13:53-05:00" level=warning msg="C:\\Users\\raghu\\IdeaProjects\\event-ledger\\docker-compose.yml: the attribute `version` is obsolete, it will be ignored, please remove it to avoid potential confusion"
[+] Building 13.3s (15/15) FINISHED                                                                docker:desktop-linux
 => [account-service internal] load build definition from Dockerfile                                               0.0s
 => => transferring dockerfile: 196B                                                                               0.0s
 => [gateway-service internal] load metadata for docker.io/library/eclipse-temurin:21-jdk-alpine                   0.7s
 => [account-service internal] load .dockerignore                                                                  0.0s
 => => transferring context: 2B                                                                                    0.0s
 => [gateway-service 1/3] FROM docker.io/library/eclipse-temurin:21-jdk-alpine@sha256:4fb80de7aeb277ad949cfbe89b4  0.1s
 => => resolve docker.io/library/eclipse-temurin:21-jdk-alpine@sha256:4fb80de7aeb277ad949cfbe89b4f504e50bb34c57fd  0.1s
 => [account-service internal] load build context                                                                  2.3s
 => => transferring context: 75.67MB                                                                               2.3s
 => CACHED [gateway-service 2/3] WORKDIR /app                                                                      0.0s
 => [account-service 3/3] COPY target/account-service-0.0.1-SNAPSHOT.jar app.jar                                   0.2s
 => [account-service] exporting to image                                                                           3.0s
 => => exporting layers                                                                                            2.4s
 => => exporting manifest sha256:367cc32450dffd41d8d70057499090c2d08355f43959d10ad1e3aa698e2262b1                  0.0s
 => => exporting config sha256:082fde2855660ca409403a27a3f428da30d446b52e2228967cbb15f0a506fd01                    0.0s
 => => exporting attestation manifest sha256:2c4d0ca2ba8fd17d9526271038d7faad5ea7cde0ed2d0372f0cf4e569261dcde      0.1s
 => => exporting manifest list sha256:be665b56993fec5a8bc5a9037baf81c6150195bcf80bed835c748ed59eda3d17             0.0s
 => => naming to docker.io/library/event-ledger-account-service:latest                                             0.0s
 => => unpacking to docker.io/library/event-ledger-account-service:latest                                          0.4s
 => [account-service] resolving provenance for metadata file                                                       0.0s
 => [gateway-service internal] load build definition from Dockerfile                                               0.0s
 => => transferring dockerfile: 196B                                                                               0.0s
 => [gateway-service internal] load .dockerignore                                                                  0.0s
 => => transferring context: 2B                                                                                    0.0s
 => [gateway-service internal] load build context                                                                  2.4s
 => => transferring context: 84.88MB                                                                               2.4s
 => [gateway-service 3/3] COPY target/gateway-service-0.0.1-SNAPSHOT.jar app.jar                                   0.3s
 => [gateway-service] exporting to image                                                                           3.4s
 => => exporting layers                                                                                            2.7s
 => => exporting manifest sha256:8b5f3cc64201abe64b8012599c7cd3f620bad7be047f08697b8799d7a7dd041a                  0.0s
 => => exporting config sha256:beb03bdfb04bd78a29c37599b10166373021195319ce39cd2227c859d63be997                    0.0s
 => => exporting attestation manifest sha256:4a10d6db5886d52cc9264f3cfe4bacafb8b45b0b5e2a328790264ed640302b4d      0.1s
 => => exporting manifest list sha256:9fcee2c7dd831cdcded2afae546ac482b69972d32b830836c51e26a6e2e2fded             0.0s
 => => naming to docker.io/library/event-ledger-gateway-service:latest                                             0.0s
 => => unpacking to docker.io/library/event-ledger-gateway-service:latest                                          0.4s
 => [gateway-service] resolving provenance for metadata file                                                       0.0s
[+] Running 6/6
 ✔ Network event-ledger-network  Created                                                                           0.1s
 ✔ Container jaeger              Started                                                                           1.0s
 ✔ Container prometheus          Started                                                                           1.0s
 ✔ Container account-service     Healthy                                                                          11.4s
 ✔ Container gateway-service     Started                                                                          11.5s
 ✔ Container grafana             Started                                                                           1.0s

3. Verify Container Statuses
To check the runtime health states and trace verification chains:

docker compose ps

PS C:\Users\raghu\IdeaProjects\event-ledger> docker compose ps
time="2026-06-08T02:18:41-05:00" level=warning msg="C:\\Users\\raghu\\IdeaProjects\\event-ledger\\docker-compose.yml: the attribute `version` is obsolete, it will be ignored, please remove it to avoid potential confusion"
NAME              IMAGE                             COMMAND                  SERVICE           CREATED         STATUS                   PORTS
account-service   event-ledger-account-service      "java -jar app.jar"      account-service   4 minutes ago   Up 4 minutes (healthy)   0.0.0.0:8081->8081/tcp
gateway-service   event-ledger-gateway-service      "java -jar app.jar"      gateway-service   4 minutes ago   Up 4 minutes             0.0.0.0:8080->8080/tcp
grafana           grafana/grafana:latest            "/run.sh"                grafana           4 minutes ago   Up 4 minutes             0.0.0.0:3000->3000/tcp
jaeger            jaegertracing/all-in-one:latest   "/go/bin/all-in-one-…"   jaeger            4 minutes ago   Up 4 minutes             4317/tcp, 9411/tcp, 14250/tcp, 0.0.0.0:4318->4318/tcp, 14268/tcp, 0.0.0.0:16686->16686/tcp
prometheus        prom/prometheus:latest            "/bin/prometheus --c…"   prometheus        4 minutes ago   Up 4 minutes             0.0.0.0:9090->9090/tcp

Accessing the Telemetry Dashboards
Once all components display a status of healthy or running, generate some test data by making an API call through the gateway (e.g., http://localhost:8080/actuator/health). Then inspect the dashboards:

1. Distributed Tracing in Jaeger
URL: http://localhost:16686

Usage: Select gateway-service from the Service dropdown on the left and click Find Traces. Click into a timeline to view end-to-end execution paths, span IDs, and propagation delays.

2. Time-Series Metrics in Prometheus
URL: http://localhost:9090

Usage: Navigate to Status -> Targets. Both microservice instances must show a green UP status, confirming active metric polling.

3. Monitoring Dashboards in Grafana
URL: http://localhost:3000

Credentials: Username: admin | Password: admin

Configuration:

Go to Connections -> Data Sources -> click Add data source.

Select Prometheus and configure the Connection URL to: http://prometheus:9090.

Scroll down and hit Save & test.

Navigate to Dashboards -> New -> Import.

Enter ID 4701 (JVM Micrometer Core) or 11378 (Spring Boot Microservices Dashboard) and click Load to initialize real-time graphs.

🔧 Windows-Specific Troubleshooting
Error: Mount /etc/prometheus/prometheus.yml: not a directory
This error happens if the file prometheus.yml is missing or has a hidden .txt extension on Windows, prompting Docker to mistakenly generate an empty directory cache.

Resolution:

    1.Shut down the stack and clear structural volumes:

        docker compose down -v

    2. Delete the accidental directory named prometheus.yml in your file browser.

    3. Force-create a clean file via PowerShell:
            New-Item -ItemType File -Name "prometheus.yml"
    4. Paste the standard scrape rules inside, save, and redeploy using :
        docker compose up --build -d.

Safe Shutdown Command
To safely terminate the network, wipe temporary containers, and release allocated RAM blocks without deleting configuration assets:

    docker compose down