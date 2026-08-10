# Backend

Spring Boot (Java 17) API backing the Android app. See
[`../docs/SAAS_ROADMAP.md`](../docs/SAAS_ROADMAP.md) for the overall
plan and rollout phases, and
[`../docs/decisions/0001-backend-language-and-framework.md`](../docs/decisions/0001-backend-language-and-framework.md)
for why this stack was chosen.

## Run locally

The service now requires Postgres. Start one with the bundled compose file:

```bash
cd backend
docker compose up -d postgres
./gradlew bootRun
```

Flyway applies the migrations in `src/main/resources/db/migration` on startup.
The compose credentials match the defaults in `application.yml`, so no
environment configuration is needed for local work.

`GET /v1/health` is public. `GET /v1/me` requires a Firebase ID token
(`Authorization: Bearer <token>`) and needs `FIREBASE_CREDENTIALS_PATH` pointed
at a service account JSON — without it, `/v1/me` returns 503 rather than
blocking startup.

## Test

```bash
./gradlew test
```

Integration tests start their own throwaway Postgres via Testcontainers, so
they need a working Docker daemon but do not touch the compose database. Every
integration test runs the migrations, so a broken migration fails the build
rather than the deploy.

## Configuration

| Variable | Default | Purpose |
| --- | --- | --- |
| `PORT` | `8080` | HTTP port |
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/carrenting` | JDBC URL |
| `DATABASE_USERNAME` | `carrenting` | Database user |
| `DATABASE_PASSWORD` | `carrenting` | Database password |
| `FIREBASE_CREDENTIALS_PATH` | unset | Service account JSON for token verification |

## Current scope

**Phase 0 — proven end to end**

- `/v1/health` — public liveness check.
- Firebase ID token verification via `FirebaseAuthFilter`.

**Phase 1 — tenant persistence (in progress)**

- Postgres via Spring Data JPA, schema owned by Flyway.
- `company` and `app_user`, mirroring the Firestore `companies` and `users`
  collections. Per open decision 1 in the roadmap the split is hybrid:
  Firestore stays the operational store the app reads and writes live, while
  Postgres becomes the system of record for tenancy and billing.
- `TenantContextService` resolves a verified uid into the caller's tenant,
  role and lifecycle status — the server-side replacement for the client's
  `TenantSessionProvider`.
- `/v1/me` now returns tenant, role, statuses and derived permissions.

Callers must check the `provisioned` flag on `/v1/me`. Until the Firestore
backfill lands most users have no mirrored row, and an unprovisioned response
reports the least-privileged value for every status rather than failing the
request.

Still to come in Phase 1: server-authoritative pricing, booking overlap
enforcement, the Firestore backfill, and the four endpoints the app already
calls (`/v1/mobile/concierge`, `/v1/mobile/notifications/email`,
`/v1/user/export`, `/v1/user/delete`).
