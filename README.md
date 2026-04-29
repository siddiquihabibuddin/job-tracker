# JobTracker

A full-stack job application tracking platform built with a microservices architecture, event-driven CQRS pattern, and a modern React frontend.

---

## Screenshots

| Login | Register |
|---|---|
| ![Login](docs/screenshots/login.png?raw=true) | ![Register](docs/screenshots/register.png?raw=true) |

| Dashboard (with AI Insights) | Applications |
|---|---|
| ![Dashboard](docs/screenshots/dashboard.png?raw=true) | ![Applications](docs/screenshots/applications.png?raw=true) |

| Application Detail | New Application |
|---|---|
| ![Application Detail](docs/screenshots/view-application.png?raw=true) | ![New Application](docs/screenshots/new-application.png?raw=true) |

| Grafana Dashboard | Prometheus Targets |
|---|---|
| ![Grafana Dashboard](docs/screenshots/grafana-dashboard.png?raw=true) | ![Prometheus Targets](docs/screenshots/prometheus-targets.png?raw=true) |

---

## Tech Stack

**Backend**

![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.6-6DB33F?style=flat-square&logo=springboot)
![Spring Cloud](https://img.shields.io/badge/Spring_Cloud-2025.0.0-6DB33F?style=flat-square&logo=spring)
![Spring AI](https://img.shields.io/badge/Spring_AI-1.0.0-6DB33F?style=flat-square&logo=spring)
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
![Redis](https://img.shields.io/badge/Redis-7-DC382D?style=flat-square&logo=redis)
![Ollama](https://img.shields.io/badge/Ollama-LLM-000000?style=flat-square)
![Prometheus](https://img.shields.io/badge/Prometheus-monitoring-E6522C?style=flat-square&logo=prometheus)
![Grafana](https://img.shields.io/badge/Grafana-11.0-F46800?style=flat-square&logo=grafana)
![Zipkin](https://img.shields.io/badge/Zipkin-tracing-FE7139?style=flat-square)

---

## Architecture

JobTracker follows an **event-driven CQRS-like pattern** — writes and reads are handled by separate services, with Kafka as the messaging backbone.

```
  Host filesystem        ┌─────────────────────────────────────────────────────────┐
  ~/csv-imports/  ───────┼──┐              Docker Network                          │
  (HOST_CSV_FOLDER)      │  │ volume mount                                         │
                         │  │ /app/csv-imports                                     │
  Browser                │  │ ┌────────────────────────────────────────────────┐  │
  :5173  ────────────────┼──┼▶│            API Gateway  :8080                  │  │
                         │  │ │       Spring Cloud Gateway (WebFlux)           │  │
                         │  │ └──────────┬──────────────────┬───────────────── ┘  │
                         │  │            │                  │                      │
                         │  │ ┌──────────▼──────────┐  ┌───▼───────────────────┐  │
                         │  └▶│  Applications Svc   │  │    Stats Service      │  │
                         │    │     :8081           │  │       :8082           │  │
                         │    │  Spring Boot + JPA  │  │  Spring Boot + JDBC  │  │
                         │    │  JWT Auth + Flyway  │  │  JWT Auth + Redis    │  │
                         │    │  CSV / Folder Import│  │  tz-aware KPI cards  │  │
                         │    └──────────┬──────────┘  └──────┬──────▲────────┘  │
                         │               │                     │      │            │
                         │    ┌──────────▼──────────┐  ┌──────▼──┐   │            │
                         │    │     PostgreSQL       │  │  Redis  │   │            │
                         │    │    jt_apps :5432    │  │  :6379  │   │            │
                         │    │  applications       │  │  5m TTL │   │            │
                         │    │  outbox_events      │  │ 30m TTL │   │            │
                         │    └─────────────────────┘  │(AI ins.)│   │            │
                         │                             └─────────┘   │            │
                         │                                            │            │
                         │    ┌─────────────────────┐  ┌─────────────┴─────────┐  │
                         │    │     PostgreSQL       │  │    Stats Listener     │  │
                         │    │    jt_stats :5432 ◀─────┤       :8083           │  │
                         │    │  applications_      │  │  Kafka Consumer       │  │
                         │    │    snapshot         │  │  2 consumer groups:   │  │
                         │    │  agg_monthly        │  │  stats-service        │  │
                         │    │  agg_weekly         │  │  activity-service     │  │
                         │    │  agg_role           │  └──────────▲────────────┘  │
                         │    │  activity_feed      │             │               │
                         │    │  stale_flags        │  ┌──────────┴────────────┐  │
                         │    └─────────────────────┘  │    Apache Kafka       │  │
                         │                             │    (KRaft) :9092      │  │
                         │                             └──────────▲────────────┘  │
                         │                                        │               │
                         │    ┌───────────────────────┐           │               │
                         │    │   OutboxPoller (5s)   │───────────┘               │
                         │    │  reads outbox_events  │                           │
                         │    │  → publishes to Kafka │                           │
                         │    └───────────────────────┘                           │
                         │                                                         │
                         │    ┌───────────────────────┐  ┌─────────────────────┐  │
                         │    │   AI Service  :8085   │  │   Config Server     │  │
                         │    │  Spring AI ChatClient │  │      :8888          │  │
                         │    │  POST /v1/ai/insights │  │  Spring Cloud Config│  │
                         │    │  POST /v1/ai/         │  └─────────────────────┘  │
                         │    │       applications/   │                            │
                         │    │       parse  (NL→DTO) │                            │
                         │    └──────────┬────────────┘                            │
                         │               │                                         │
                         │    ┌──────────▼────────────┐                            │
                         │    │   Ollama  :11434      │                            │
                         │    │  qwen2.5:1.5b         │                            │
                         │    │  local LLM, no API key│                            │
                         │    └───────────────────────┘                            │
                         │                                                         │
                         │    ┌─────────────────────┐  ┌──────────────────────┐  │
                         │    │  Prometheus  :9090   │◀─┤   Grafana  :3001     │  │
                         │    │  scrapes /actuator/  │  │  pre-built dashboard │  │
                         │    │  prometheus (15s)    │  │  JVM · HTTP · Kafka  │  │
                         │    └──────────▲───────────┘  └──────────────────────┘  │
                         │               │ metrics                                 │
                         │    apps · stats · stats-listener · ai-service           │
                         │                                                         │
                         │    Zipkin :9411  (distributed tracing, 100% sampling)  │
                         └─────────────────────────────────────────────────────────┘
```

### Services

| Service | Port | Description |
|---|---|---|
| **api-gateway** | 8080 | Single entry point — routes traffic, handles CORS globally |
| **applications-service** | 8081 | Write service — REST CRUD, JPA + Hibernate, publishes Kafka events |
| **stats-service** | 8082 | Read service — analytics from pre-computed agg tables, Redis-cached; serves activity feed |
| **stats-listener** | 8083 | Kafka consumer — two groups: `stats-service` (snapshot/agg) + `activity-service` (activity feed) |
| **ai-service** | 8085 | LLM-backed AI service — Spring AI `ChatClient` against Ollama. Powers two endpoints: dashboard insights (pulls per-user analytics from stats-service, forwards the JWT) and natural-language application parsing (`POST /v1/ai/applications/parse` — extracts a structured DTO from a free-text description, no persistence) |
| **config-server** | 8888 | Centralized config for all services (Spring Cloud Config) |
| **PostgreSQL** | 5432 | Two databases: `jt_apps` (write) and `jt_stats` (read) |
| **Kafka** | 9092 | KRaft mode, topic: `application-events`, two consumer groups |
| **Redis** | 6379 | Cache for stats endpoints (5-min TTL) and AI insights (30-min TTL) |
| **Ollama** | 11434 | Local LLM server — serves `qwen2.5:1.5b` for AI insights generation |
| **Prometheus** | 9090 | Scrapes `/actuator/prometheus` from all Spring Boot services |
| **Grafana** | 3001 | Pre-built dashboard: request rates, JVM heap, HTTP latency, Kafka lag |
| **Zipkin** | 9411 | Distributed tracing, 100% sampling |

### Event Flow

1. `applications-service` creates/updates/deletes an application → writes an `ApplicationEvent` row to the `outbox_events` table **in the same DB transaction** as the application save
2. `OutboxPoller` (scheduled every 5s) reads unpublished outbox rows → publishes each to Kafka synchronously → marks `published_at`
3. `stats-listener` consumes events via **two independent consumer groups**:
   - `stats-service` group → upserts into `applications_snapshot`, atomically recomputes `agg_monthly`/`agg_weekly`
   - `activity-service` group → translates each event into a human-readable message, inserts into `activity_feed` (idempotent via unique constraint)
4. `stats-service` queries `agg_monthly` / `agg_weekly` for indexed reads; results are cached in Redis for 5 minutes
5. `GET /v1/stats/activity/{appId}` serves the per-application activity timeline to the frontend

This is the **Transactional Outbox Pattern** — Kafka being down never causes data loss. Events accumulate safely in Postgres and drain automatically when Kafka recovers. The `stats-listener` uses `ON CONFLICT` upserts so duplicate delivery on poller restart is safe.

---

## Features

- **User registration & login** — `POST /v1/auth/register` creates a new account (BCrypt-hashed password, immediate JWT); `POST /v1/auth/token` authenticates existing users; all credentials stored in the `users` table — no hardcoded demo users
- **JWT Authentication** — HS256 token-based auth, enforced by Spring Security on all services; each user sees only their own data
- **FREE / PREMIUM tiers** — Every account is `FREE` by default. The `/upgrade` page presents a mock checkout (no real charge); submitting valid card details flips the account to `PREMIUM` and re-issues a fresh JWT carrying a `tier` claim so every service gates access locally without a DB round-trip. Server-side enforcement (`PremiumGuard.requirePremium()`) in `applications-service` and `ai-service` returns HTTP 402 with `{ "error": "premium_required", "upgradeUrl": "/upgrade" }` for free users hitting premium endpoints; the frontend 402 interceptor redirects to `/upgrade`
- **Full CRUD** — Create, read, update, and soft-delete job applications with rich fields: apply date, salary range, job link, call received, reject date, resume, login details, notes
- **CSV Import** *(Premium)* — Bulk import applications from a spreadsheet export; handles quoted commas, multiple date formats, salary parsing (`$50K`, `50,000–80,000`), and flexible status mapping (`Open` → APPLIED, `Closed` → REJECTED, Open + Call → PHONE). Re-importing the same file is safe: rows with a matching `(company, role, applied_at, resume_uploaded)` key are updated instead of duplicated; rows missing `applied_at` or `resume_uploaded` are always inserted. The response includes an `updated` count alongside `imported` and `failed`
- **Folder-based Bulk Import** *(Premium)* — Drop CSV files into a configured host folder (`HOST_CSV_FOLDER` in `.env`, mounted into the container at `/app/csv-imports`) and click "Bulk Import" in the UI. Each file is processed individually and moved to a `processed/` subfolder after import. The results modal shows per-file imported/updated/failed counts with expandable row-level errors. Same deduplication logic applies per file
- **Bulk delete** *(Premium)* — Select any number of applications via per-row checkboxes or the select-all header checkbox and delete them all in one request (`POST /v1/applications/bulk-delete`); server soft-deletes each owned application, publishes a Kafka event per deletion, and returns `{ deleted, skipped }`
- **Advanced filtering** — Filter applications by status, search (company/role), month, year, and call received; sort by apply date or date added; page-based pagination (20 per page)
- **Activity Feed** — Each Application Detail page shows a live activity timeline. Every create, status update, and deletion is translated into a human-readable message (e.g. "Applied for SWE at Google via LinkedIn", "Status changed to OFFER") and stored in the `activity_feed` table. Powered by Kafka fan-out: a second consumer group (`activity-service`) runs in `stats-listener` alongside the existing `stats-service` group, tracking independent offsets on the same `application-events` topic — no producer changes required. Idempotent via a unique constraint on `(app_id, event_type, occurred_at)`
- **AI Insights** *(Premium)* — The Dashboard includes an "AI Insights" card powered by a locally-running LLM (Ollama + `qwen2.5:1.5b`). A dedicated **`ai-service`** (port 8085) handles the LLM round-trip using Spring AI's `ChatClient` abstraction — it pulls per-user analytics from `stats-service` via the gateway (forwarding the caller's JWT), stuffs them into a prompt template, and asks the model for 3–5 concise, actionable coaching insights. Endpoint: `POST /v1/ai/insights`. Fully offline — no API key required
- **Smart Create (Natural-Language Application Entry)** *(Premium)* — On the New Application page, a "✦ Smart create with AI" section accepts a free-text sentence like *"Today I applied to a senior engineer position at Microsoft using LinkedIn with a salary range of $100,000 to $200,000"* and extracts a structured DTO (company, role, status, source, salary range, currency, applied date, location, job link, tags, notes). The same `ai-service` handles the parsing: `POST /v1/ai/applications/parse` returns parsed fields **without persisting** so the user can review and edit the form before saving via the existing create flow. The frontend merges parsed values non-destructively (never overwrites a field the user has already typed). Built on the same Spring AI `ChatClient` + Ollama stack as AI Insights — fully offline, no API key
- **Analytics Dashboard** — By Month / By Year toggle with grouped Applied vs Rejected bar chart, summary table, 7 open-window KPI cards (last 7d / 15d / 30d / 3mo / 6mo / 9mo / 1yr), and a **Top Companies** widget ranking the top 10 companies by application count with a relative fill bar for quick visual comparison
- **Pre-computed aggregate tables** — `agg_monthly` and `agg_weekly` in `jt_stats` are maintained in-sync by stats-listener (recomputed atomically on every Kafka event); stats-service reads from these tables with indexed scans instead of live GROUP BY on the raw snapshot
- **Redis caching** — All three stats endpoints (`/summary`, `/trend`, `/breakdown`) are cached in Redis with a 5-minute TTL, keyed per-user; the stats-listener evicts the affected user's cache keys immediately after processing each Kafka event, so the dashboard always reflects the latest data
- **Idempotent writes** — All POST/PATCH endpoints require an `Idempotency-Key` header
- **Soft deletes** — Applications are logically deleted (`deleted_at` timestamp); all queries filter accordingly
- **Transactional Outbox Pattern** — Events are written to `outbox_events` in the same DB transaction as the application save; a scheduled poller publishes them to Kafka, guaranteeing no event loss even if Kafka is temporarily unavailable
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
POST /v1/auth/register
Body:     { "email": "you@example.com", "password": "••••••••", "displayName": "Your Name" }
Response: 201 { "token": "eyJ...", "email": "...", "userId": "...", "displayName": "...", "tier": "FREE" }

POST /v1/auth/token
Body:     { "email": "you@example.com", "password": "••••••••" }
Response: 200 { "token": "eyJ...", "email": "...", "userId": "...", "displayName": "...", "tier": "FREE|PREMIUM" }
```

Passwords are BCrypt-hashed (strength 10). Emails are normalised to lowercase before storage and lookup. Duplicate email registration returns `409 { "error": "Email already registered" }`. Invalid credentials return `401 { "error": "Invalid credentials" }` (same message for all failure branches to prevent user enumeration).

The JWT carries a `tier` claim (`"FREE"` or `"PREMIUM"`) in addition to `sub` (userId UUID) and `email`. All four Spring Boot services read this claim locally via `NimbusJwtDecoder` — no service-to-service call needed to check tier. Tokens are valid for 24 hours; upgrading via `/v1/billing/checkout` issues a fresh token immediately so the new tier takes effect without waiting for expiry.

### Billing

```
POST /v1/billing/checkout   (auth required)
Body:     { "cardNumber": "4242 4242 4242 4242", "expMonth": 12, "expYear": 2030,
            "cvc": "123", "nameOnCard": "Jane Smith" }
Response: 200 { "token": "eyJ...", "email": "...", "userId": "...", "displayName": "...", "tier": "PREMIUM" }
```

Mock billing — no real charge is made and no card data is persisted. Spaces and dashes in `cardNumber` are stripped before format validation (`@Pattern(\d{13,19})`). On success the user's tier is updated to `PREMIUM` in the database and a fresh JWT is returned; the frontend stores the new token and invalidates the React Query cache so premium UI unlocks immediately.

```
GET /v1/billing/me   (auth required)
Response: { "tier": "FREE|PREMIUM", "tierUpdatedAt": "2026-04-29T..." }
```

### Applications

```
GET    /v1/applications?status=APPLIED&search=google&month=4&year=2025&gotCall=true&sortBy=appliedAt&page=0&limit=20
POST   /v1/applications                     (Idempotency-Key header required)
GET    /v1/applications/{id}
PATCH  /v1/applications/{id}                (Idempotency-Key header required)
DELETE /v1/applications/{id}

POST   /v1/applications/import              (multipart/form-data, field: file)  [Premium]
POST   /v1/applications/import-folder       (processes all .csv files in HOST_CSV_FOLDER)  [Premium]
POST   /v1/applications/bulk-delete         (body: { "ids": ["uuid", ...] })  [Premium]

POST   /v1/applications/{id}/notes
PATCH  /v1/applications/{appId}/notes/{noteId}
```

Application statuses: `APPLIED` → `PHONE` → `ONSITE` → `OFFER` → `ACCEPTED / REJECTED / WITHDRAWN`

**CSV import** — the file must have this header row (column order matters):

```
Company,Role,Location,Salary Range,Apply Date,Final Status,Job Link,Resume Uploaded,Call,Reject Date,Login Details,Days pending
```

Response: `{ imported: N, updated: M, failed: K, errors: ["row 3: ...", ...] }`

### Analytics

```
GET /v1/stats/summary?window=30d
Response: { windowDays, totalApplied, byStatus: {...}, bySource: {...}, generatedAt }

GET /v1/stats/trend?period=week&weeks=12
Response: { period, points: [{ start, end, count }] }

GET /v1/stats/breakdown?groupBy=month&year=2025&tz=America/Phoenix
GET /v1/stats/breakdown?groupBy=year
Response: {
  groupBy, year,
  rows: [{ label, periodNum, totalApplied, totalRejected, totalOpen }],
  openWindows: { today, last7d, last15d, last30d, last3m, last6m, last9m, last1y }
}
# tz is the IANA timezone name sent by the browser (Intl.DateTimeFormat().resolvedOptions().timeZone)
# openWindows.today counts active applications applied on the user's local calendar date

GET /v1/stats/roles?groupBy=month&year=2025
GET /v1/stats/roles?groupBy=year
Response: {
  groupBy, year,
  rows: [{ label, periodNum, engineerDev, manager, architect, other }]
}

GET /v1/stats/insights
Response: { insights: ["...", "...", "..."], generatedAt }
# Legacy in-process implementation served by stats-service; cached in Redis (30-min TTL)

POST /v1/ai/insights
Body:     { "windowDays": 30 }   # optional, defaults to 30
Response: { insights: ["...", "...", "..."], generatedAt }
# New Spring AI implementation served by ai-service. Aggregates summary, 12-week trend,
# current-year monthly breakdown, and role distribution by calling stats-service over HTTP
# (auth header is forwarded), then asks Ollama via ChatClient for 3-5 insights

POST /v1/ai/applications/parse
Body:     { "description": "Today I applied to a senior engineer position at Microsoft using LinkedIn with a salary range of $100,000 to $200,000" }
Response: 200 {
  company?: string, role?: string, status?: AppStatus,
  source?: string, location?: string,
  salaryMin?: number, salaryMax?: number, currency?: string,
  appliedAt?: string, nextFollowUpOn?: string,
  jobLink?: string, tags?: string[], notes?: string
}
# Returns a structured DTO extracted from a free-text description. NO persistence — the
# frontend uses this to prefill the New Application form so the user can review/edit
# before saving via POST /v1/applications. Fields the model cannot confidently extract
# are simply omitted. 400 on blank/oversized input, 502 if the LLM call fails or
# returns unparseable JSON.

GET /v1/stats/activity/{appId}
Response: [{ id, eventType, message, occurredAt }]

GET /v1/stats/stale
Response: [{ appId, company, role, status, daysSinceLastEvent, flaggedAt, appliedAt }]
# Returns applications with no status change for 14+ days (ghosted / stale)

GET /v1/stats/companies
Response: [{ company, count }]
# Top 10 companies by non-deleted application count, descending; Redis-cached per user
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
`postgres + kafka + redis + config-server + ollama` → `applications-service + stats-service + stats-listener + ollama-init` → `ai-service` (waits on `ollama` healthy + `stats-service`) → `api-gateway`

> **First run note:** An `ollama-init` container automatically pulls `qwen2.5:1.5b` (~1GB) on first startup. The AI Insights card shows a graceful fallback message until the download completes. The model is cached in the `ollama_data` Docker volume and is not re-downloaded on subsequent runs.

### 3. Start the frontend

```bash
cd JobTracker/frontend
npm install
npm run dev
# Available at http://localhost:5173
```

### 4. Log in or create an account

Navigate to `http://localhost:5173/register` to create a new account, or sign in at `/login`.

A demo account is pre-seeded: **email:** `demo@example.com` | **password:** `demo`

---

## Project Structure

```
JobTracker/
├── backend/
│   ├── api-gateway/            # Spring Cloud Gateway (WebFlux)
│   ├── applications-service/   # Write service — JPA, Kafka producer
│   ├── stats-service/          # Read service — agg table queries, Redis-cached responses
│   ├── stats-listener/         # Kafka consumer — snapshot upserts + agg table maintenance
│   ├── ai-service/             # Spring AI ChatClient → Ollama, /v1/ai/insights
│   ├── config-server/          # Spring Cloud Config Server
│   ├── docker-compose.yml      # Full infrastructure definition
│   ├── init-db.sql             # Creates jt_stats DB (runs on fresh volume)
│   ├── prometheus.yml          # Prometheus scrape config
│   ├── grafana/                # Grafana provisioning
│   │   ├── provisioning/       # Auto-wired datasource + dashboard loader
│   │   └── dashboards/         # Pre-built Job Tracker dashboard JSON
│   ├── .env.example            # Required environment variables
│   └── .gitignore
└── frontend/
    ├── src/
    │   ├── api/                # Axios clients + typed API functions (applications, billing, stats)
    │   ├── auth/               # AuthContext (with tier), RequireAuth, usePremium hook
    │   ├── components/         # PremiumGate, UpsellCard, shared UI components
    │   ├── routes/             # Login, Register, Dashboard, Applications, Detail, NewApplication,
    │   │                       #   Profile, Upgrade (mock checkout)
    │   └── main.tsx            # App entry + ErrorBoundary
    ├── .env                    # Local dev environment variables
    └── package.json
```

---

## Database Schema

**`jt_apps`** (write database)

- `users` — registered users with BCrypt-hashed passwords, optional display name, `tier TEXT DEFAULT 'FREE'` (CHECK `IN ('FREE','PREMIUM')`), and `tier_updated_at`
- `applications` — job applications with JSONB tags, salary range, soft-delete, and extended fields: `applied_at`, `job_link`, `resume_uploaded`, `got_call`, `reject_date`, `login_details`
- `application_status_history` — full audit trail of status transitions
- `application_notes` — notes per application
- `outbox_events` — transactional outbox; events written here atomically with application mutations, polled and published to Kafka every 5s

**`jt_stats`** (read database)

- `applications_snapshot` — denormalized projection of applications, kept in sync via Kafka; includes `applied_at` for accurate date-based analytics
- `agg_monthly` — pre-computed application counts keyed by `(user_id, year, month, status)`; updated atomically on every Kafka event; enables `breakdown` and month-level open-window queries with a single indexed scan
- `agg_weekly` — pre-computed weekly counts keyed by `(user_id, week_start)`; updated atomically on every Kafka event; enables `trend` queries with a fast indexed range scan
- `agg_role` — pre-computed application counts keyed by `(user_id, year, month, category)`; category is one of `ENGINEER_DEV`, `MANAGER`, `ARCHITECT`, `OTHER`; powers the role breakdown chart
- `activity_feed` — per-application event timeline; one row per Kafka event, translated to a human-readable message; idempotent via unique constraint on `(app_id, event_type, occurred_at)`
- `stale_flags` — tracks applications with no status change for 14+ days; populated by a scheduled job in `stats-listener`; powers the "Possibly Ghosting" section on the dashboard

All schema changes are managed by Flyway migrations (`ddl-auto: validate`).

---

## Configuration

All Spring Boot services pull their runtime config from the Config Server at startup. Config files live in:

```
backend/config-server/src/main/resources/config-repo/
├── applications-service.yml
├── stats-service.yml
├── stats-listener.yml
├── ai-service.yml
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

| Tool | URL | Credentials | Description |
|---|---|---|---|
| Grafana | http://localhost:3001 | admin / admin | Pre-built dashboard: app rates, JVM heap, HTTP latency p99, Kafka consumer lag |
| Prometheus | http://localhost:9090 | — | Scrapes all 3 Spring Boot services every 15s; data persisted via named volume |
| Zipkin | http://localhost:9411 | — | Distributed trace viewer, 100% sampling |
| Actuator (apps) | http://localhost:8081/actuator | — | Health, metrics, Prometheus endpoint |
| Actuator (stats) | http://localhost:8082/actuator | — | Health, metrics, Prometheus endpoint |
| Actuator (listener) | http://localhost:8083/actuator | — | Health, metrics, Prometheus endpoint |
| Actuator (ai) | http://localhost:8085/actuator | — | Health, metrics, Prometheus endpoint |

Custom Micrometer counters: `applications_total`, `applications_deleted_total`, `stats_queries_total`

All services emit structured logs with a `correlationId` and `traceId` included in every log line.
