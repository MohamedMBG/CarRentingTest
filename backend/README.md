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

If Testcontainers cannot reach your Docker daemon — its named-pipe discovery
does not always find Docker Desktop on Windows — point the tests at any empty
throwaway Postgres 16 instead:

```bash
docker run -d --name cr-test -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=carrenting_test -p 55433:5432 postgres:16-alpine

TEST_DATABASE_URL=jdbc:postgresql://localhost:55433/carrenting_test \
TEST_DATABASE_USERNAME=postgres TEST_DATABASE_PASSWORD=postgres ./gradlew test
```

The database must be empty the first time: Flyway will not baseline a
populated schema it has no history for. CI leaves these unset and uses the
container.

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

**Phase 1 — fleet and server-authoritative booking (in progress)**

- `car` and `rental_request` tables (migration V2).
- `GET /v1/cars` — the caller's own fleet. Renters see bookable vehicles;
  an active admin may pass `?includeUnavailable=true` to see the rest.
- `POST /v1/bookings/quote` — price a car for a period.
- `POST /v1/bookings` — create a pending booking. The total is computed from
  the `car` row; there is no price field on the request, so a modified client
  cannot choose what it pays.
- `GET /v1/bookings` — the tenant's queue for an admin, the caller's own
  bookings otherwise.
- `POST /v1/bookings/{id}/approve|reject|complete` — admin-only transitions,
  validated against the state machine in `RentalRequestStatus`.

Approving is what takes a vehicle off the market. Two approved bookings cannot
overlap on the same car: the rule is a Postgres exclusion constraint
(`rental_request_no_overlapping_hold`), not an application check, so concurrent
approvals cannot both succeed. Pending requests deliberately do not hold the
vehicle — several renters may ask for the same dates and the agency chooses.

Refusals carry a stable `code` alongside the message (`dates_already_held`,
`booking_not_permitted`, `car_not_bookable`, `illegal_status_transition`,
`invalid_rental_period`) so clients can branch on the reason without parsing
prose.

**Phase 1 — Firestore mirror (in progress)**

- `FirestoreGateway` reads `companies`, `users` and `cars` from Firestore. It
  is read-only by design: the app still owns those documents, and a second
  writer without a shared transaction produces divergence nobody can
  reconstruct.
- `TenantMirrorService` upserts them into Postgres, companies first — a user
  referencing an unmirrored company is rejected by the foreign key.
- A verified caller with no mirrored row is provisioned from Firestore on
  their first request, so `/v1/me` and the booking endpoints work without
  waiting for a backfill. If Firestore is unreachable the caller simply stays
  `provisioned: false`, as before.
- `POST /v1/tenant/sync` mirrors the caller's whole tenant — company, users
  and fleet — and reports the counts. Active agency admins only, and it takes
  no tenant parameter: the company synced is always the caller's.

Without Firebase credentials configured the gateway reports itself
unavailable and every mirror is a no-op, which is why the service still boots
and tests run with no Firebase at all.

Still to come in Phase 1: cutover of the app's booking writes, and the four
endpoints the app already calls
(`/v1/mobile/concierge`, `/v1/mobile/notifications/email`, `/v1/user/export`,
`/v1/user/delete`).
