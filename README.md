# Transaction Risk & Fraud Detection Platform

[![Java CI](https://github.com/your-org/fraud-detection-platform/actions/workflows/ci.yml/badge.svg)](https://github.com/your-org/fraud-detection-platform/actions)
![Java 21](https://img.shields.io/badge/Java-21-orange.svg)
![Spring Boot 3.3](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen.svg)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg)
![Redis](https://img.shields.io/badge/Redis-7-red.svg)
![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)

A production-style, modular backend engineered in **Java 21** and **Spring Boot 3.x** that models real-time transaction risk scoring and fraud prevention. Built following **Clean Architecture**, **SOLID principles**, strict layer decoupling, and algorithmic signal analysis.

> **DISCLAIMER**: This platform is an educational and portfolio reference implementation demonstrating rule-based risk engines, Spring Boot architectural patterns, and security best practices. It is **not** a certified banking-grade fraud detection system and does not replace dedicated machine-learning enterprise solutions.

---

## Table of Contents

- [1. Overview & Problem Statement](#1-overview--problem-statement)
- [2. System Architecture](#2-system-architecture)
- [3. Technology Stack](#3-technology-stack)
- [4. Risk Detection Engine & Algorithms](#4-risk-detection-engine--algorithms)
- [5. Database Schema & Flyway Migrations](#5-database-schema--flyway-migrations)
- [6. Redis High-Throughput Signals](#6-redis-high-throughput-signals)
- [7. REST API Reference](#7-rest-api-reference)
- [8. Sample End-to-End Walkthrough](#8-sample-end-to-end-walkthrough)
- [9. Setup & Local Execution](#9-setup--local-execution)
- [10. Docker & Docker Compose](#10-docker--docker-compose)
- [11. Testing & Verification](#11-testing--verification)
- [12. CI/CD Pipeline](#12-cicd-pipeline)
- [13. Security Considerations](#13-security-considerations)
- [14. Limitations & Future Roadmap](#14-limitations--future-roadmap)
- [15. Publishing to GitHub](#15-publishing-to-github)

---

## 1. Overview & Problem Statement

Financial transactions are vulnerable to account takeovers, card testing, credential stuffing, and location spoofing. Evaluating transactions based solely on amounts or static rules misses multi-vector fraud patterns.

This platform implements a **multi-signal, rule-based risk engine** that concurrently evaluates transaction attributes against user behavioral baselines, device fingerprints, geographic proximity, merchant risk registries, and sliding-window Redis frequency counters.

### Core Capabilities
* **Stateless JWT Security**: Role-based access control (`ROLE_USER`, `ROLE_ANALYST`, `ROLE_ADMIN`) with BCrypt password hashing.
* **Pluggable Risk Engine**: Strategy pattern where each fraud rule is an isolated, testable, Spring-managed component.
* **Sub-Millisecond Fast-Path**: Redis sliding-window counters for rate limiting and rapid transaction velocity tracking without database lock contention.
* **Immutable Audit Trail**: Asynchronous event recording for compliance, forensic investigation, and triage.
* **Automated Triage Pipeline**: Dynamic categorization into `LOW`, `MEDIUM`, `HIGH`, or `CRITICAL` risk with corresponding automated decisions (`APPROVED`, `REVIEW`, `BLOCKED`).

---

## 2. System Architecture

The application adopts a **Clean Layered Architecture** with strict directional dependency enforcement:

```
                          HTTP Request
                               │
                               ▼
                   ┌───────────────────────┐
                   │   JwtAuthFilter       │
                   └───────────┬───────────┘
                               │ Authenticated Principal
                               ▼
                   ┌───────────────────────┐
                   │     Controllers       │  <── Spring Validation / OpenAPI
                   └───────────┬───────────┘
                               │ Request DTOs
                               ▼
                   ┌───────────────────────┐
                   │      Services         │  <── MapStruct Mappers
                   └─────┬───────────┬─────┘
                         │           │
           ┌─────────────┘           └──────────────┐
           ▼                                        ▼
┌───────────────────────┐               ┌───────────────────────┐
│     Risk Engine       │               │      Data Repos       │
│  ┌─────────────────┐  │               └───────────┬───────────┘
│  │ Pluggable Rules │  │                           │
│  │ (Strategy)      │  │                           ▼
│  └─────────────────┘  │               ┌───────────────────────┐
└──────────┬────────────┘               │  PostgreSQL Database  │
           │                            │  (Flyway Migrations)  │
           ▼                            └───────────────────────┘
┌───────────────────────┐
│     Redis Cache       │
│ (Sliding Windows)     │
└───────────────────────┘
```

### Package Organization

```
com.frauddetection
├── config/           # Spring beans, SecurityConfig, RedisConfig, RiskConfig, OpenApiConfig
├── controller/       # REST endpoints (Auth, Transactions, Risk, Admin, Dashboard, Users)
├── dto/              # Immutable Java Records for Request/Response payloads
│   ├── request/      # RegisterRequest, LoginRequest, TransactionRequest
│   └── response/     # AuthResponse, TransactionResponse, RiskAssessmentResponse, etc.
├── entity/           # JPA Entities (User, Role, Transaction, Device, Merchant, RiskEvent, AuditLog)
├── exception/        # Custom exceptions and @RestControllerAdvice GlobalExceptionHandler
├── mapper/           # Compile-time MapStruct mappers (no reflection overhead)
├── repository/       # Spring Data JPA repositories with custom JPQL queries and indexes
├── risk/             # Core Fraud Risk Evaluation Engine
│   ├── engine/       # RiskEngine orchestrator
│   └── rule/         # 7 concrete RiskRule implementations (Strategy Pattern)
├── security/         # JwtUtil, JwtAuthFilter, UserDetailsServiceImpl
├── service/          # Business workflows (Auth, Transaction, User, Merchant, Audit, Redis)
└── util/             # GeoUtil (Haversine formula), ApiErrorResponse
```

---

## 3. Technology Stack

| Layer / Concern | Technology | Selection Rationale |
|---|---|---|
| **Runtime** | Java 21 LTS | Pattern matching, records, virtual-thread readiness, modern syntax |
| **Framework** | Spring Boot 3.3.4 | Ecosystem stability, Spring Data JPA, Spring Security 6 |
| **Persistence** | PostgreSQL 16 | ACID compliance, JSON/UUID support, indexed relational schema |
| **Migrations** | Flyway | Reproducible, version-controlled database schema migrations |
| **Caching / State** | Redis 7 (Lettuce) | Sliding-window transaction frequency counters, API rate limiting |
| **Security** | Spring Security + JJWT 0.12.6 | Stateless JWT authorization, BCrypt salted hashing |
| **Validation** | Jakarta Bean Validation | Declarative API request parameter validation |
| **Mapping** | MapStruct 1.6.2 | Compile-time type-safe entity-DTO conversions without reflection |
| **Documentation** | SpringDoc OpenAPI 3 / Swagger UI | Interactive API playground with Bearer JWT support |
| **Testing** | JUnit 5 + Mockito + AssertJ | Comprehensive unit and mock-based testing |
| **Containerization** | Docker + Docker Compose | Multi-stage builder images with Alpine JRE runtime |
| **CI/CD** | GitHub Actions | Automated build, test, and artifact packaging on push |

---

## 4. Risk Detection Engine & Algorithms

The risk engine is built on the **Strategy Pattern**. All rules implement the `RiskRule` interface:

```java
public interface RiskRule {
    RuleEvaluation evaluate(RiskContext context);
    String getName();
}
```

When a transaction is submitted, the `RiskEngine` compiles a `RiskContext` capturing:
- Transaction amount, coordinates, merchant, and device fingerprint.
- User transaction history and baseline average transaction size.
- User device registry (distinguishing new vs. trusted devices).
- Redis sliding-window transaction counters.
- Recent failure counters.

### Configurable Scoring System

All weights and thresholds are externalized in `application.yml` and bound to `RiskConfig`:

| Rule Name | Target Signal | Config Key | Default Weight | Condition |
|---|---|---|---|---|
| **LargeTransactionRule** | Amount Anomaly | `large-transaction-score` | `+20` | Amount > `3.0x` user average |
| **TransactionFrequencyRule** | Rapid / Burst Txns | `frequency-anomaly-score` | `+20` | ≥ `10` txns in `60` min window |
| **LocationAnomalyRule** | Geographic Anomaly | `location-anomaly-score` | `+25` | Distance > `500 km` from recent transaction |
| **NewDeviceRule** | Unseen Device | `new-device-score` | `+15` | Unrecognized device fingerprint |
| **SuspiciousIpRule** | Flagged Network | `suspicious-ip-score` | `+20` | Matches configured CIDR/IP blacklist |
| **HighRiskMerchantRule** | Risky Category | `high-risk-merchant-score` | `+15` | Merchant marked `HIGH` risk in registry |
| **FailedTransactionRule** | Card Testing / Brute Force | `failed-transaction-score` | `+10` | ≥ `3` failed txns in `30` min window |

### Normalization and Decision Matrix

$$\text{Final Risk Score} = \min\left(100, \sum_{i=1}^{n} \text{RuleScore}_i\right)$$

```
┌──────────────┬────────────────┬───────────────────┐
│ Score Range  │  Risk Level    │ Automated Action  │
├──────────────┼────────────────┼───────────────────┤
│    0 – 29    │  LOW           │ APPROVED          │
│   30 – 59    │  MEDIUM        │ APPROVED          │
│   60 – 79    │  HIGH          │ REVIEW            │
│   80 – 100   │  CRITICAL      │ BLOCKED           │
└──────────────┴────────────────┴───────────────────┘
```

> **Simplification Documentation**: 
> * **Location Anomaly**: Employs the great-circle Haversine formula (`GeoUtil.distanceKm`) comparing the current transaction coordinates against recent coordinates within the evaluation window. It flags transitions exceeding 500 km. It does not compute time-velocity travel matrices (e.g. flight speed feasibility); this is intentional for portfolio simplicity.
> * **Suspicious IP**: Configurable through the `SUSPICIOUS_IPS` environment variable rather than external BGP or threat-intelligence feeds.

---

## 5. Database Schema & Flyway Migrations

Database tables and indexes are strictly managed by versioned Flyway migrations under `src/main/resources/db/migration`:

* **`V1__create_tables.sql`**: Creates `roles`, `users`, `user_roles`, `merchants`, `transactions`, `devices`, `risk_events`, and `audit_logs` using PostgreSQL native `UUID` and `pgcrypto`.
* **`V2__create_indexes.sql`**: Applies composite and single-column B-Tree indexes optimized for query patterns:
  - `idx_txn_user_timestamp` (`user_id`, `transaction_timestamp`)
  - `idx_txn_status` (`status`)
  - `idx_txn_risk_level` (`risk_level`)
  - `idx_device_user_fingerprint` (unique on `user_id`, `device_fingerprint`)
  - `idx_audit_event_type`, `idx_audit_created_at`
* **`V3__insert_roles.sql`**: Seeds base roles (`ROLE_USER`, `ROLE_ANALYST`, `ROLE_ADMIN`).
* **`V4__seed_data.sql`**: Seeds ready-to-test scenarios (admin, analyst, regular users, sample merchants, devices, and 4 pre-calculated transactions illustrating LOW, MEDIUM, HIGH, and CRITICAL levels).

---

## 6. Redis High-Throughput Signals

Redis is intentionally integrated only where in-memory sliding-window performance is critical:

1. **Transaction Velocity Tracking (`txn:count:<userId>:<windowMinutes>`)**: Atomic `INCR` with a TTL window. Detects carding and burst attacks without running table-scan `COUNT(*)` queries on PostgreSQL.
2. **Failed Attempt Tracking (`txn:failed:<userId>`)**: Monitors repeated card rejection or authentication failure sequences.
3. **API Rate Limiting (`rate:<ipAddress>`)**: Prevents endpoint abuse by limiting clients to 60 requests/minute.
4. **Temporary Risk Signal Caching (`risk:score:<userId>`)**: 5-minute cache for fast lookup during consecutive actions.

---

## 7. REST API Reference

All requests expecting authentication must include `Authorization: Bearer <token>`.

### Authentication
* `POST /api/auth/register` — Create account (`ROLE_USER`)
* `POST /api/auth/login` — Authenticate and receive JWT

### Transactions & Risk
* `POST /api/transactions` — Submit a transaction (triggers Risk Engine)
* `GET /api/transactions/{id}` — Get transaction details
* `GET /api/transactions/user/{userId}` — List user transaction history (paginated)
* `GET /api/risk/transaction/{transactionId}` — Get detailed score breakdown and triggered rules

### Admin & Operations (`ROLE_ADMIN`, `ROLE_ANALYST`)
* `GET /api/admin/transactions/high-risk` — Paginated list of HIGH/CRITICAL transactions
* `GET /api/admin/transactions/suspicious` — Recent suspicious transactions for triage
* `GET /api/admin/risk-statistics` — System-wide risk score distribution and counts
* `GET /api/dashboard/stats` — High-level metric aggregates for dashboards
* `GET /api/dashboard/suspicious` — Live fraud alert feed
* `GET /api/users` — List system users

---

## 8. Sample End-to-End Walkthrough

### 1. Authenticate (Login)
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "password": "password"
  }'
```

**Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "username": "john_doe",
  "email": "john@example.com",
  "userId": "33333333-3333-3333-3333-333333333333",
  "roles": ["ROLE_USER"]
}
```

---

### 2. Normal Transaction (LOW Risk → APPROVED)
```bash
curl -X POST http://localhost:8080/api/transactions \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 75.00,
    "currency": "USD",
    "merchant": "Amazon",
    "transactionType": "PURCHASE",
    "deviceId": "device-known-001",
    "location": "New York, NY",
    "latitude": 40.7128,
    "longitude": -74.0060,
    "ipAddress": "192.168.1.15"
  }'
```

**Response (201 Created):**
```json
{
  "id": "e2f17042-4dc8-4a94-8144-8848d2df41e0",
  "userId": "33333333-3333-3333-3333-333333333333",
  "username": "john_doe",
  "amount": 75.00,
  "currency": "USD",
  "merchant": "Amazon",
  "transactionType": "PURCHASE",
  "status": "APPROVED",
  "riskScore": 0,
  "riskLevel": "LOW"
}
```

---

### 3. Suspicious Transaction (HIGH Risk → REVIEW)
```bash
curl -X POST http://localhost:8080/api/transactions \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 2500.00,
    "currency": "USD",
    "merchant": "Crypto Exchange XYZ",
    "transactionType": "PURCHASE",
    "deviceId": "device-unknown-xyz",
    "location": "Los Angeles, CA",
    "latitude": 34.0522,
    "longitude": -118.2437,
    "ipAddress": "192.168.1.15"
  }'
```

**Response (201 Created):**
```json
{
  "id": "7b897fd9-0bca-443b-a91c-1f5be602b988",
  "userId": "33333333-3333-3333-3333-333333333333",
  "username": "john_doe",
  "amount": 2500.00,
  "currency": "USD",
  "merchant": "Crypto Exchange XYZ",
  "transactionType": "PURCHASE",
  "status": "REVIEW",
  "riskScore": 75,
  "riskLevel": "HIGH"
}
```

---

### 4. Inspect Detailed Risk Breakdown
```bash
curl -X GET http://localhost:8080/api/risk/transaction/7b897fd9-0bca-443b-a91c-1f5be602b988 \
  -H "Authorization: Bearer <TOKEN>"
```

**Response (200 OK):**
```json
{
  "transactionId": "7b897fd9-0bca-443b-a91c-1f5be602b988",
  "riskScore": 75,
  "riskLevel": "HIGH",
  "decision": "REVIEW",
  "reasons": [
    "Unusual transaction amount (2500.00 exceeds 3.0x average of 150.00)",
    "Suspicious location change: 3936 km from recent transaction",
    "Transaction from a previously unseen device: device-unknown-xyz",
    "Transaction at a high-risk merchant: Crypto Exchange XYZ"
  ]
}
```

---

## 9. Setup & Local Execution

### Prerequisites
* **Java 21 LTS**
* **Maven 3.9+** (or use bundled wrapper)
* **PostgreSQL 16** (listening on port 5432)
* **Redis 7** (listening on port 6379)

### Environment Configuration
Copy the template configuration file:
```bash
cp .env.example .env
```

| Variable | Description | Default |
|---|---|---|
| `DB_URL` | JDBC Connection String | `jdbc:postgresql://localhost:5432/fraud_detection` |
| `DB_USERNAME` | Database User | `fraud_user` |
| `DB_PASSWORD` | Database Password | `changeme` |
| `REDIS_HOST` | Redis Hostname | `localhost` |
| `REDIS_PORT` | Redis Port | `6379` |
| `JWT_SECRET` | 256-bit HMAC secret key | *(Required in Production)* |
| `JWT_EXPIRATION_MS` | Access token lifespan (ms) | `86400000` (24h) |
| `SUSPICIOUS_IPS` | Comma-delimited list of flagged IPs | `192.168.100.1,10.0.0.1` |

### Running Locally
```bash
# 1. Build and compile
mvn clean package -DskipTests

# 2. Run the Spring Boot application
java -jar target/fraud-detection-platform-1.0.0.jar
```

Access Swagger UI at: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)  
OpenAPI Specification at: [http://localhost:8080/api-docs](http://localhost:8080/api-docs)

---

## 10. Docker & Docker Compose

Run the entire cluster (PostgreSQL, Redis, Spring Boot Application) with a single command:

```bash
# Start all services in detached mode
docker compose up --build -d

# View real-time application logs
docker compose logs -f app

# Stop the environment
docker compose down
```

The database initializes automatically, applies Flyway migrations `V1` through `V4`, seeds initial data, and registers health check probes.

---

## 11. Testing & Verification

Run the complete test suite:

```bash
# Run all unit and service tests
mvn test

# Run tests with detailed failure logs
mvn test -Dtest=*Test
```

### Coverage Scope
* **`RiskEngineTest`**: Validates scoring normalization, score bounds (LOW/MEDIUM/HIGH/CRITICAL), automated status resolution, and Redis counter side-effects.
* **`LargeTransactionRuleTest`**: Tests multiplier edge cases and new accounts with zero historical baseline.
* **`LocationAnomalyRuleTest`**: Tests Haversine calculations over intra-city vs. inter-continental distances and missing coordinates.
* **`TransactionFrequencyRuleTest`**: Tests sliding-window frequency boundary checks.
* **`NewDeviceRuleTest`**: Verifies fingerprint matching and first-seen device handling.
* **`SuspiciousIpRuleTest`**: Tests IP matching against configured blocklists.
* **`AuthServiceTest`**: Verifies password hashing, duplicate email/username rejection, and security audit log recording.
* **`TransactionServiceTest`**: Validates orchestration, device tracking persistence, and transaction lifecycle.
* **`AuthControllerTest`**: `MockMvc` web-layer test validating validation annotations, request parsing, and HTTP status codes.

---

## 12. CI/CD Pipeline

The `.github/workflows/ci.yml` pipeline triggers on every push and pull request to `main`:
1. Provisions Ubuntu runners with active PostgreSQL and Redis service containers.
2. Configures JDK 21 Temurin with Maven dependency caching.
3. Executes `mvn clean verify -B` to run the test suite against live services.
4. Packages the production JAR and archives it as an action build artifact.

---

## 13. Security Considerations

* **Credential Protection**: Passwords hashed using standard BCrypt with automatic salting. Passwords and JWT tokens are never logged.
* **Non-Enumeration**: Authentication failure endpoints return generic `"Invalid username or password"` responses to prevent account scanning.
* **Resource Ownership Enforcement**: Users are restricted to their own transactions via programmatic checks and method security (`@PreAuthorize`).
* **Input Validation**: All incoming requests are guarded with Jakarta Bean Validation (`@NotNull`, `@Size`, `@DecimalMin`, `@Email`).
* **Least Privilege Containers**: Dockerfile runs the application as an unprivileged `appuser` within an Alpine Linux container.

---

## 14. Limitations & Future Roadmap

* **Travel Feasibility**: The location rule presently checks static distance thresholds. A future improvement would calculate velocity ($v = \Delta d / \Delta t$) against commercial airline speeds.
* **Machine Learning Integration**: Complement heuristic rules with an ML model (e.g. Isolation Forest or XGBoost) via an ONNX runtime.
* **Kafka Event Streaming**: Decouple ingestion and scoring using Apache Kafka for 50,000+ txns/sec event streaming.
* **Device Fingerprinting Complexity**: Expand simple client-provided device IDs into multi-attribute browser canvas/hardware signatures.

---

## 15. Publishing to GitHub

To push this repository to GitHub:

```bash
cd C:\Users\ojasg\.gemini\antigravity\scratch\fraud-detection-platform

# Initialize git repository
git init

# Add files
git add .

# Commit
git commit -m "feat: initial commit of Transaction Risk & Fraud Detection Platform"

# Link remote and push
git branch -M main
git remote add origin https://github.com/<your-username>/fraud-detection-platform.git
git push -u origin main
```

---

*Authored as an advanced portfolio project demonstrating modern Java 21, Spring Boot 3, and defensive distributed systems design.*
