# 0001 — Backend language and framework: Java + Spring Boot

## Context

`docs/BACKEND_API_PLAN.md` requires a real backend to enforce tenant
isolation server-side, own billing, and stop the Android app writing
sensitive/billable data straight to Firestore. The plan left the
language/framework as an open decision (Kotlin+Spring Boot, Kotlin+Ktor, and
Node+NestJS were the options considered), to be settled before any backend
code was written.

## Decision

Build the backend in **Java 17 with Spring Boot 3**, as a standalone Gradle
project under `backend/` in this same repository (not a separate repo, not a
Gradle multi-module of the Android app).

## Why

- The Android app is already Java-majority (with some Kotlin); Java keeps
  the whole stack in one language the team already works in daily, rather
  than introducing Kotlin-on-backend or a second ecosystem (Node) on top of
  the existing Java/Kotlin Android codebase.
- Spring Boot has first-class support for everything Phase 0–3 of the
  backend plan need: REST controllers, the Firebase Admin SDK (Google's
  official Java SDK), the Stripe Java SDK for Phase 3 billing, and Spring
  Data JPA for the Postgres tenant/billing schema in Phase 1 — all without
  needing third-party glue.
- Keeping it in the same repo (`backend/` directory, own Gradle build, own
  CI job scoped by path filter) avoids the overhead of cross-repo versioning
  for a project this size, while still being a fully independent Gradle
  project (own `settings.gradle`, own wrapper) so it builds/deploys on its
  own schedule, separate from Android releases.

## Consequences

- Easier: code review and onboarding stay single-language; Firebase
  Admin SDK and Stripe integration have mature, well-documented Java APIs;
  CI can gate on `backend/**` path changes only, keeping Android and backend
  pipelines independent despite sharing a repo.
- Harder: no shared DTOs/type generation between Android (Java/Kotlin) and
  backend (Java) the way a schema-first approach (e.g. protobuf/OpenAPI
  codegen) might give for free — this is deferred; Phase 1 should introduce
  an OpenAPI spec once the API surface stabilizes rather than hand-writing
  matching DTOs indefinitely.
- If the repo ever needs independent deploy cadences/access control for the
  backend (e.g. a different team owns it), splitting `backend/` into its own
  repo later is a straightforward extraction since it has no build-time
  dependency on the Android module today.
