# BricoPro Backend

**BricoPro** is the backend for a Moroccan home-services marketplace — think TaskRabbit, localized for Morocco — connecting **clients** who need work done (plumbing, cleaning, painting, repairs, moving, etc.) with verified **workers** who do it. Built with Spring Boot 3, it covers the full lifecycle: registration & CIN identity verification, task posting, worker matching & bidding, in-app messaging, payments, ratings, and an admin back-office.

> 📄 For the complete endpoint-by-endpoint reference, see **[`API_DOCUMENTATION.md`](./API_DOCUMENTATION.md)**.
> 🔍 For a full code audit (bugs, security findings, missing features, duplicated logic), see **[`AUDIT_REPORT.md`](./AUDIT_REPORT.md)**.

---

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
  - [1. Clone & configure](#1-clone--configure)
  - [2. Run with Docker Compose (recommended)](#2-run-with-docker-compose-recommended)
  - [3. Run locally without Docker](#3-run-locally-without-docker)
- [Environment Variables](#environment-variables)
- [Database Migrations](#database-migrations)
- [API Documentation](#api-documentation)
- [Authentication](#authentication)
- [Running Tests](#running-tests)
- [Third-Party Service Setup](#third-party-service-setup)
- [Contributing](#contributing)

---

## Features

- **Auth**: email/phone registration, OTP verification, JWT access + refresh tokens, Google/Facebook OAuth2, account lockout after repeated failed logins.
- **Worker verification**: Moroccan CIN (national ID) upload with Tesseract OCR extraction + admin manual review.
- **Task lifecycle**: post a task, browse/accept (or bid on) it, track status through completion, leave a review.
- **Bidding & negotiation**: workers bid, clients can counter, workers can revise, full negotiation history.
- **Smart matching**: AI-style scoring (distance, rating, response rate, experience, premium/CIN boosts) to recommend the best workers for a task.
- **Geolocation**: Haversine-based "workers near me" search.
- **Messaging**: REST + WebSocket (STOMP) real-time chat per task.
- **Payments**: commission-based fee split (12% platform + 1.5% processing), CMI gateway scaffolding for Moroccan card payments, cash flow.
- **Notifications**: in-app, push (FCM), email, and WhatsApp (via Green API).
- **Engagement & growth**: badges, referrals, worker subscriptions (Free/Premium/Enterprise), sponsored listings, group bookings, recurring tasks.
- **Admin back-office**: worker verification queue, dispute resolution, user management, CSV/PDF exports, platform analytics & fraud/churn signals.
- **Localization**: French and Arabic throughout (`messages_fr.properties`, `messages_ar.properties`).

## Tech Stack

| Layer | Technology |
|---|---|
| Language / Runtime | Java 17, Spring Boot 3.4.5 |
| Web | Spring Web MVC, Spring WebFlux (`WebClient` for OAuth2) |
| Security | Spring Security, JWT (jjwt 0.12.6), OAuth2 Client |
| Persistence | Spring Data JPA / Hibernate, MySQL 8, Flyway migrations |
| Caching / Locking | Redis, Caffeine (local cache), ShedLock (distributed scheduler locking) |
| Real-time | WebSocket + STOMP (SockJS fallback) |
| Rate limiting | Bucket4j (token bucket, per-endpoint) |
| Mapping | MapStruct + Lombok |
| Documents | iText 9 (PDF export) |
| OCR | Tesseract (`tess4j`) for CIN verification |
| API docs | springdoc-openapi (Swagger UI) |
| Build | Maven |
| Containerization | Docker, Docker Compose |
| Testing | JUnit 5, Spring Boot Test, Spring Security Test, H2 (in-memory test DB) |

## Project Structure

The codebase is organized by **domain feature**, each typically with its own `controller/`, `service/`, `repository/`, `entity/`, and `dto/` sub-packages under `com.bricopro.<feature>`:

```
src/main/java/com/bricopro/
├── auth/             # Registration, login, OTP, OAuth2, refresh tokens
├── security/         # JwtAuthFilter, JwtService, rate limiting, OAuth2 success handler —
│                     #   the root cause of AUDIT_REPORT.md Bugs #1 and #19 lives here
├── user/             # Users, worker/client profiles, availability, favorites, portfolio,
│                     #   streaks (UserStreak), recent-workers, home-screen digest
├── task/             # Task CRUD, lifecycle, reviews, cancellation, rating-based suspension
├── bidding/          # Bid placement, negotiation, expiration scheduler
├── payment/          # Payments, CMI/Cash gateways, platform revenue
├── messaging/        # Conversations, messages, WebSocket delivery
├── notification/     # In-app + push (FCM) notifications
├── matching/         # Worker scoring/matching, real-time auto-assignment
├── geolocation/      # Nearby-worker search (Haversine)
├── search/           # Unified home-screen search
├── activity/         # Recent client activity feed
├── analytics/        # Dashboards (admin/worker/client) + advanced analytics module
├── admin/            # Admin actions, CSV/PDF exports
├── recommendation/   # Personalized recommendations
├── verification/     # CIN OCR verification
├── estimation/       # Instant price estimation
├── pricing/          # Surge pricing
├── subscription/     # Worker subscriptions, sponsored listings, recurring tasks
├── badge/            # Achievement badges
├── banner/           # Promotional banners
├── referral/         # Referral codes & rewards
├── trust/            # Trust/safety signals
├── tracking/         # Live worker GPS tracking
├── preference/       # User preferences, categories, i18n
├── service/          # Database-backed service catalog (ServiceCategory) — see
│                     #   AUDIT_REPORT.md Bug #6 for how this relates to `preference`'s
│                     #   categories endpoint and the ServiceType enum
├── insights/         # City-level trending-services insights
├── home/             # Shared DTOs for home-screen widgets (banners, activity, search, etc.)
├── offline/          # Offline action queue replay/sync endpoint
├── booking/          # Group bookings
├── upload/           # File upload (avatars, CIN docs, task/portfolio photos)
├── common/           # Cross-cutting utilities (XSS filter, HTML sanitizer)
├── i18n/             # Locale/translation configuration
└── config/           # Security filter chain wiring, WebSocket, caching, async, scheduling
```

> ℹ️ Eight feature areas (badge, booking, referral, subscription ×2, preference, tracking, advanced analytics) are currently implemented as single consolidated `*Module.java` files rather than split across the standard sub-packages. They work correctly but are a planned cleanup target — see `AUDIT_REPORT.md` item #15.

## Prerequisites

- **JDK 17+** (the Docker image currently builds with JDK 21 — see `AUDIT_REPORT.md` #13 for the discrepancy)
- **Maven** (or use the bundled `./mvnw` wrapper — no separate install needed)
- **MySQL 8+** (or use the bundled Docker Compose service)
- **Redis** (required for login-attempt tracking, ShedLock, and caching)
- **Docker & Docker Compose** (recommended path — see below)

## Getting Started

### 1. Clone & configure

```bash
git clone https://github.com/anasshatim1997/BricoProBackend.git
cd BricoProBackend
cp .env.example .env   # then fill in real values — see below
```

> ⚠️ No `.env.example` currently ships in this repository even though `.gitignore` expects one (`!.env.example`). Use the template under [Environment Variables](#environment-variables) below to create your own `.env.example` / `.env` until one is committed (see `AUDIT_REPORT.md` #14).

### 2. Run with Docker Compose (recommended)

This spins up MySQL and the Spring Boot app together, with the app waiting for MySQL's healthcheck before starting:

```bash
docker compose up --build
```

The API will be available at `http://localhost:8080` (or whatever `SERVER_PORT` you set in `.env`). Swagger UI: `http://localhost:8080/swagger-ui.html`.

### 3. Run locally without Docker

```bash
# 1. Start MySQL and Redis yourself, then export the variables from .env
#    (or use a tool like `direnv` / `set -a; source .env; set +a`)

# 2. Run database migrations + start the app
./mvnw spring-boot:run
```

Flyway runs automatically on startup (`spring.flyway.enabled: true`), applying all scripts under `src/main/resources/db/migration` in order.

## Environment Variables

The application reads all configuration from environment variables (see `src/main/resources/application.yml`). Required variables have no default and **must** be set; optional ones show their default.

```dotenv
# ── App ──────────────────────────────────────────────────────────
APP_ENV=dev
SERVER_PORT=8080
APP_BASE_URL=http://localhost:8080

# ── Database (required) ─────────────────────────────────────────
DB_URL=jdbc:mysql://localhost:3306/bricopro_dev?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true
DB_USERNAME=root
DB_PASSWORD=changeme
DB_POOL_SIZE=10

# ── JWT (required) ───────────────────────────────────────────────
# Generate with: openssl rand -base64 64
JWT_SECRET=replace-with-a-real-base64-secret-at-least-64-bytes
JWT_ACCESS_EXP_MS=900000        # 15 minutes
JWT_REFRESH_EXP_MS=604800000    # 7 days

# ── OAuth2 social login (required to enable Google/Facebook login) ─
GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=
FACEBOOK_CLIENT_ID=
FACEBOOK_CLIENT_SECRET=

# ── Mail (required for OTP/password-reset/notification emails) ──
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-smtp-username@gmail.com
MAIL_PASSWORD=your-smtp-app-password

# ── File uploads ─────────────────────────────────────────────────
UPLOAD_BASE_DIR=/var/bricopro/uploads

# ── WhatsApp via Green API (optional, free tier: 1500 msg/month) ─
# Sign up at https://green-api.com
WHATSAPP_ENABLED=false
GREENAPI_INSTANCE_ID=
GREENAPI_TOKEN=

# ── Firebase Cloud Messaging push notifications (optional) ──────
# Firebase Console → Project Settings → Cloud Messaging
FCM_SERVER_KEY=

# ── CMI payment gateway (optional — see AUDIT_REPORT.md #2 before
#    relying on this; the gateway is currently NOT invoked by the
#    payment service even when these are set) ─────────────────────
PAYMENT_CMI_CLIENT_ID=
PAYMENT_CMI_STORE_KEY=
PAYMENT_CMI_CALLBACK_URL=https://api.yourdomain.com/api/v1/payments/webhook/cmi
PAYMENT_CMI_OK_URL=https://yourapp.com/payment/success
PAYMENT_CMI_FAIL_URL=https://yourapp.com/payment/fail
PAYMENT_CMI_TEST_MODE=true

# ── OCR (CIN verification) ───────────────────────────────────────
TESSDATA_PATH=/usr/share/tesseract-ocr/5/tessdata
OCR_LANGUAGE=fra

# ── OAuth2 mobile deep-link redirect ─────────────────────────────
APP_OAUTH2_REDIRECT_URI=brico://oauth2/callback
```

> Variable names map to `application.yml` placeholders one-to-one except for nesting — e.g. `payment.cmi.client-id` → `PAYMENT_CMI_CLIENT_ID` via Spring's relaxed binding, and `app.oauth2.redirect-uri` → `APP_OAUTH2_REDIRECT_URI`.

## Database Migrations

Schema is managed by **Flyway**. Migrations live in `src/main/resources/db/migration/` and run in order on every startup:

| Version | Purpose |
|---|---|
| `V1__init_schema.sql` | Core schema: users, tasks, payments, messaging, etc. |
| `V2__add_reliability_fields.sql` | Reliability score / cancellation tracking fields |
| `V3__add_bidding_tables.sql` | Bid table and related indexes |
| `V4__add_negotiation_fields.sql` | Counter-offer / revision support for bids |
| `V5__add_home_screen_tables.sql` | Banners, activity, recommendations |
| `V6__add_user_streaks.sql` | Engagement streak tracking |
| `V7__add_cin_verification_fields.sql` | CIN OCR verification fields |

`spring.jpa.hibernate.ddl-auto` is set to `validate` — Hibernate will **not** auto-generate schema changes; every change must go through a new Flyway migration file (`V8__...sql`, etc.).

## API Documentation

- **Swagger UI** (interactive, generated from code annotations): `/swagger-ui.html`
- **Raw OpenAPI spec**: `/v3/api-docs`
- **Full written reference** with request/response examples for every endpoint: [`API_DOCUMENTATION.md`](./API_DOCUMENTATION.md)

## Authentication

1. `POST /api/v1/auth/register` → creates the account (OTP sent if phone provided).
2. `POST /api/v1/auth/verify-otp` (if applicable) → activates the account.
3. `POST /api/v1/auth/login` → returns `accessToken` (15 min) + `refreshToken` (7 days).
4. Send `Authorization: Bearer <accessToken>` on every subsequent request.
5. `POST /api/v1/auth/refresh` with the refresh token to get a new pair once the access token expires.

Social login is available via `GET /oauth2/authorization/google` and `GET /oauth2/authorization/facebook` (standard browser redirect flow), landing back on a custom mobile deep link with tokens attached as query parameters.

## Running Tests

```bash
./mvnw test
```

Tests use an in-memory H2 database and Spring Security Test utilities. **Note:** the bidding test suite (`BiddingControllerTest`) currently targets the wrong URL prefix (`/api/bids` instead of `/api/v1/bids`) and uses a mock principal type that doesn't match production — see `AUDIT_REPORT.md` items #1 and #12 before trusting a green run of that specific suite as proof the bidding feature works end-to-end.

## Third-Party Service Setup

| Service | Used for | Setup |
|---|---|---|
| **Google OAuth2** | Social login | [Google Cloud Console](https://console.cloud.google.com/) → OAuth consent screen + credentials |
| **Facebook OAuth2** | Social login | [Facebook for Developers](https://developers.facebook.com/) → create an app, add Facebook Login |
| **SMTP (e.g. Gmail)** | OTP/transactional email | Use an [App Password](https://myaccount.google.com/apppasswords) if using Gmail with 2FA |
| **Green API** | WhatsApp notifications | [green-api.com](https://green-api.com) → connect a WhatsApp number, copy instance ID + token |
| **Firebase Cloud Messaging** | Push notifications | Firebase Console → Project Settings → Cloud Messaging → copy server key |
| **CMI** | Card payments (Morocco) | [cmi.co.ma](https://www.cmi.co.ma) → "Espace Commerçant" → submit RC + RIB + ID, receive merchant ID + store key (⚠️ see `AUDIT_REPORT.md` #2 — integration code exists but is not yet wired into the payment flow) |
| **Tesseract OCR** | CIN document text extraction | Install `tesseract-ocr` with the French (`fra`) language pack on the host/container |

## Contributing

1. Create a feature branch off `main`.
2. Add/update tests for any behavior change.
3. Add a new Flyway migration for any schema change — never rely on `ddl-auto`.
4. Run `./mvnw test` before opening a PR.
5. Reference the relevant `AUDIT_REPORT.md` item number in your PR description if your change fixes one of the tracked findings.
