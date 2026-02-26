# JobTracker

A full-stack job application tracking platform built with a microservices architecture, event-driven CQRS pattern, and a modern React frontend.

---

## Screenshots

| Login | Dashboard |
|---|---|
| ![Login](docs/screenshots/login.png) | ![Dashboard](docs/screenshots/dashboard.png) |

| Applications | Application Detail |
|---|---|
| ![Applications](docs/screenshots/applications.png) | ![Application Detail](docs/screenshots/view-application.png) |

| New Application |
|---|
| ![New Application](docs/screenshots/new-application.png) |

---

## Tech Stack

**Backend**

![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.6-6DB33F?style=flat-square&logo=springboot)
![Spring Cloud](https://img.shields.io/badge/Spring_Cloud-2025.0.0-6DB33F?style=flat-square&logo=spring)
![Apache Kafka](https://img.shields.io/badge/Apache_Kafka-3.7.0-231F20?style=flat-square&logo=apachekafka)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-336791?style=flat-square&logo=postgresql)
![Flyway](https://img.shields.io/badge/Flyway-migrations-CC0200?style=flat-square&logo=flyway)

**Frontend**

![React](https://img.shields.io/badge/React-19-61DAFB?style=flat-square&logo=react)
![TypeScript](https://img.shields.io/badge/TypeScript-5-3178C6?style=flat-square&logo=typescript)
![Vite](https://img.shields.io/badge/Vite-7-646CFF?style=flat-square&logo=vite)
![TanStack Query](https://img.shields.io/badge/TanStack_Query-5-FF4154?style=flat-square&logo=reactquery)
![Tailwind CSS](https://img.shields.io/badge/Tailwind_CSS-4-38B2AC?style=flat-square&logo=tailwindcss)

**Infrastructure**

![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=flat-square&logo=docker)
![Prometheus](https://img.shields.io/badge/Prometheus-monitoring-E6522C?style=flat-square&logo=prometheus)
![Zipkin](https://img.shields.io/badge/Zipkin-tracing-FE7139?style=flat-square)

---

## Architecture

JobTracker follows an **event-driven CQRS-like pattern** — writes and reads are handled by separate services, with Kafka as the messaging backbone.

```
                        ┌─────────────────────────────────────────────────────────┐
                        │                     Docker Network                      │
                        │                                                         │
  Browser               │   ┌──────────────────────────────────────────────────┐ │
  :5173  ───────────────┼──▶│              API Gateway  :8080                  │ │
                        │   │         Spring Cloud Gateway (WebFlux)           │ │
                        │   └──────────┬───────────────────┬───────────────────┘ │
                        │              │                   │                      │
                        │   ┌──────────▼──────────┐  ┌────▼──────────────────┐  │
                        │   │  Applications Svc   │  │     Stats Service     │  │
                        │   │     :8081           │  │        :8082          │  │
                        │   │  Spring Boot + JPA  │  │  Spring Boot + JDBC  │  │
                        │   │  JWT Auth + Flyway  │  │  JWT Auth + Flyway   │  │
                        │   └──────────┬──────────┘  └────────────▲──────────┘  │
                        │              │                           │              │
                        │   ┌──────────▼──────────┐  ┌────────────┴──────────┐  │
                        │   │     PostgreSQL       │  │     PostgreSQL        │  │
                        │   │     jt_apps :5432   │  │    jt_stats :5432     │  │
                        │   └─────────────────────┘  └────────────▲──────────┘  │
                        │                                          │              │
                        │   ┌──────────────────────┐  ┌───────────┴──────────┐  │
                        │   │    Apache Kafka       │  │   Stats Listener     │  │
                        │   │    (KRaft) :9092  ───────▶     :8083           │  │
                        │   └──────────▲───────────┘  │  Kafka Consumer      │  │
                        │              │               └──────────────────────┘  │
                        │   ┌──────────┴───────────┐                            │
                        │   │  Applications Svc    │  Config Server :8888       │
                        │   │  publishes events    │  Prometheus    :9090       │
                        │   └──────────────────────┘  Zipkin        :9411       │
                        └─────────────────────────────────────────────────────────┘
```

### Services

| Service | Port | Description |
|---|---|---|
| **api-gateway** | 8080 | Single entry point — routes traffic, handles CORS globally |
| **applications-service** | 8081 | Write service — REST CRUD, JPA + Hibernate, publishes Kafka events |
| **stats-service** | 8082 | Read service — analytics queries on denormalized snapshot table |
| **stats-listener** | 8083 | Kafka consumer — consumes events, upserts `applications_snapshot` |
| **config-server** | 8888 | Centralized config for all services (Spring Cloud Config) |
| **PostgreSQL** | 5432 | Two databases: `jt_apps` (write) and `jt_stats` (read) |
| **Kafka** | 9092 | KRaft mode, topic: `application-events` |
| **Prometheus** | 9090 | Scrapes `/actuator/prometheus` from all Spring Boot services |
| **Zipkin** | 9411 | Distributed tracing, 100% sampling |

### Event Flow

1. `applications-service` creates/updates/deletes an application → publishes `ApplicationEvent` JSON to Kafka
2. `stats-listener` consumes the event → upserts into `applications_snapshot` in `jt_stats`
3. `stats-service` queries the snapshot table for fast, join-free analytics

---

## Features

- **JWT Authentication** — HS256 token-based auth, enforced by Spring Security on all services
- **Full CRUD** — Create, read, update, and soft-delete job applications
- **Analytics Dashboard** — Application counts by status and source, 12-week trend chart
- **Idempotent writes** — All POST/PATCH endpoints require an `Idempotency-Key` header
- **Soft deletes** — Applications are logically deleted (`deleted_at` timestamp); all queries filter accordingly
- **Event-driven read model** — The stats snapshot is asynchronously kept in sync via Kafka, keeping write and read paths fully decoupled
- **Kafka dead-letter queue** — Failed consumer messages are retried 3x then routed to a DLQ topic
- **Observability** — Structured logging with correlation IDs, Micrometer Prometheus metrics, Zipkin distributed tracing
- **Centralized config** — All Spring Boot services pull runtime configuration from a Config Server on startup
- **Production-ready Docker** — All containers run as non-root, health checks gate startup order, resource limits enforced
- **Schema migrations** — Flyway manages all DB schema; `ddl-auto: validate` prevents accidental drift
- **Input validation** — `@Validated` on all controllers with field-level `@Size`, `@Min/@Max`, `@NotBlank` constraints

---

## API Reference

### Authentication

```
POST /v1/auth/token
Body: { "email": "demo@example.com", "password": "demo" }
Response: { "token": "eyJ...", "email": "...", "userId": "..." }
```

### Applications

```
GET    /v1/applications?status=APPLIED&search=google&page=0&limit=20
POST   /v1/applications                     (Idempotency-Key header required)
GET    /v1/applications/{id}
PATCH  /v1/applications/{id}                (Idempotency-Key header required)
DELETE /v1/applications/{id}

POST   /v1/applications/{id}/notes
PATCH  /v1/applications/{appId}/notes/{noteId}
```

Application statuses: `APPLIED` → `PHONE` → `ONSITE` → `OFFER` → `ACCEPTED / REJECTED / WITHDRAWN`

### Analytics

```
GET /v1/stats/summary?window=30d
Response: { windowDays, totalApplied, byStatus: {...}, bySource: {...}, generatedAt }

GET /v1/stats/trend?period=week&weeks=12
Response: { period, points: [{ start, end, count }] }
```

Swagger UI available at `http://localhost:8081/swagger-ui.html` and `http://localhost:8082/swagger-ui.html`.

---

## Getting Started

### Prerequisites

- Docker & Docker Compose
- Node.js 20+ (for frontend)
- Java 21 + Maven (for running services individually)

### 1. Configure environment

```bash
cd JobTracker/backend
cp .env.example .env
# Edit .env — set DB_PASSWORD and JWT_SECRET (min 32 chars)
```

### 2. Start the backend

```bash
cd JobTracker/backend

# First run — wipe volume so init-db.sql creates the jt_stats database
docker compose down -v
docker compose up --build

# Subsequent runs (no rebuild needed)
docker compose up
```

Docker startup order is automatically enforced:
`postgres + kafka + config-server` → `applications-service + stats-service + stats-listener` → `api-gateway`

### 3. Start the frontend

```bash
cd JobTracker/frontend
npm install
npm run dev
# Available at http://localhost:5173
```

### 4. Log in

Use the demo credentials: **email:** `demo@example.com` | **password:** `demo`

---

## Project Structure

```
JobTracker/
├── backend/
│   ├── api-gateway/            # Spring Cloud Gateway (WebFlux)
│   ├── applications-service/   # Write service — JPA, Kafka producer
│   ├── stats-service/          # Read service — JdbcTemplate analytics
│   ├── stats-listener/         # Kafka consumer — snapshot upserts
│   ├── config-server/          # Spring Cloud Config Server
│   ├── docker-compose.yml      # Full infrastructure definition
│   ├── init-db.sql             # Creates jt_stats DB (runs on fresh volume)
│   ├── prometheus.yml          # Prometheus scrape config
│   ├── .env.example            # Required environment variables
│   └── .gitignore
└── frontend/
    ├── src/
    │   ├── api/                # Axios clients + typed API functions
    │   ├── auth/               # AuthContext + RequireAuth route guard
    │   ├── routes/             # Login, Dashboard, Applications, Detail, Profile
    │   └── main.tsx            # App entry + ErrorBoundary
    ├── .env                    # Local dev environment variables
    └── package.json
```

---

## Database Schema

**`jt_apps`** (write database)

- `users` — registered users
- `applications` — job applications with JSONB tags, salary range, soft-delete
- `application_status_history` — full audit trail of status transitions
- `application_notes` — notes per application

**`jt_stats`** (read database)

- `applications_snapshot` — denormalized projection of applications, kept in sync via Kafka; powers all analytics queries

All schema changes are managed by Flyway migrations (`ddl-auto: validate`).

---

## Configuration

All Spring Boot services pull their runtime config from the Config Server at startup. Config files live in:

```
backend/config-server/src/main/resources/config-repo/
├── applications-service.yml
├── stats-service.yml
├── stats-listener.yml
└── api-gateway.yml
```

Secrets (DB password, JWT secret) are supplied via `backend/.env` and injected as environment variables into Docker containers — never committed to source control.

---

## Running Individual Services

```bash
# From any service directory (requires local Postgres + Kafka)
mvn spring-boot:run

# Build and test
mvn clean install

# Single test class
mvn test -Dtest=ApplicationsServiceTest
```

---

## Observability

| Tool | URL | Description |
|---|---|---|
| Prometheus | http://localhost:9090 | Metrics for all Spring Boot services |
| Zipkin | http://localhost:9411 | Distributed trace viewer |
| Actuator (apps) | http://localhost:8081/actuator | Health, metrics, Prometheus endpoint |
| Actuator (stats) | http://localhost:8082/actuator | Health, metrics, Prometheus endpoint |

Custom Micrometer counters: `applications.created.total`, `applications.deleted.total`, `stats.queries.total`

All services emit structured logs with a `correlationId` and `traceId` included in every log line.
