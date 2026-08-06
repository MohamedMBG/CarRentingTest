# Backend

Spring Boot (Java 17) API backing the Android app. See
[`../docs/BACKEND_API_PLAN.md`](../docs/BACKEND_API_PLAN.md) for the overall
plan and rollout phases, and
[`../docs/decisions/0001-backend-language-and-framework.md`](../docs/decisions/0001-backend-language-and-framework.md)
for why this stack was chosen.

## Run locally

```bash
cd backend
./gradlew bootRun
```

The service boots without any external dependencies. `GET /v1/health` is
public. `GET /v1/me` requires a Firebase ID token (`Authorization: Bearer
<token>`) and needs `FIREBASE_CREDENTIALS_PATH` pointed at a service account
JSON — without it, `/v1/me` returns 503 rather than blocking startup.

## Test

```bash
./gradlew test
```

## Current scope (Phase 0)

- `/v1/health` — public liveness check.
- `/v1/me` — verifies the caller's Firebase ID token and echoes their uid.
  This is intentionally the smallest possible slice proving
  Android → backend → Firebase Admin token verification works end to end;
  it does not yet return tenant/company/role data (that's Phase 1, once
  Postgres-backed tenant tables exist).

No database is wired up yet — see the ADR for why.
