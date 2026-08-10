# SaaS Roadmap

**Status:** draft — living document
**Last updated:** 2026-08-10
**Scope:** what B&B Luxury Cars needs in order to run as a commercial, multi-tenant SaaS product rather than a single-operator Android application.

---

## Table of contents

- [1. Where the project stands today](#1-where-the-project-stands-today)
- [2. Target product shape](#2-target-product-shape)
- [3. Gap analysis](#3-gap-analysis)
- [4. Delivery phases](#4-delivery-phases)
- [5. Cross-cutting concerns](#5-cross-cutting-concerns)
- [6. Open decisions](#6-open-decisions)
- [7. Definition of done](#7-definition-of-done)

---

## 1. Where the project stands today

### 1.1 Android application — substantially built

The mobile client is the mature part of the system. It is a Java Android app (min SDK 30, target SDK 35) talking directly to Firebase.

| Area | State | Key sources |
| --- | --- | --- |
| Authentication | Firebase Auth, email/password | `SignInActivity`, `SignUpActivity` |
| Tenant model | `companyId` scoping on every domain document | `data/session/TenantContext.java`, `TenantSessionProvider.java` |
| Roles & lifecycle | `UserRole`, `UserLifecycleStatus`, `CompanyLifecycleStatus` | `domain/` |
| Authorization | Tenant-scoped Firestore security rules | `firestore.rules` |
| Fleet management | Admin CRUD over vehicles | `admin/ManageCarsActivity` |
| Booking | Request creation, status tracking, history | `RentalFormActivity`, `fragments/RequestsHistoryFragment` |
| Pricing | Days × daily rate, MAD currency; extras and discounts stubbed at zero | `pricing/PricingService.java` |
| KYC / identity | Licence capture, selfie, on-device FaceNet match, admin review queue | `ui/verification/`, `verification/data/`, `assets/facenet.tflite` |
| Privacy / GDPR | Consent screen, privacy centre, age gate, data-rights UI | `privacy/` |
| Reporting | Admin and client rental reports | `admin/AdminReportsActivity`, `admin/ClientReportsActivity` |
| Tests | 8 JVM unit tests (pricing, access, age gate, storage paths, verification) | `app/src/test/` |

Release builds are already gated on a configured HTTPS backend URL and on the `applicationId` having been moved off the placeholder `com.example.*` namespace.

### 1.2 Backend — Phase 0 scaffold only

| Item | State |
| --- | --- |
| Stack | Spring Boot 3.3.4, Java 17, Gradle (Groovy DSL) |
| Endpoints | `GET /v1/health` (public), `GET /v1/me` (Firebase ID token → uid echo) |
| Database | None. JPA and Postgres dependencies deliberately not yet added |
| Auth | `FirebaseAuthFilter` verifies Firebase ID tokens via Admin SDK |
| Packaging | `Dockerfile` present; no deploy target configured |
| CI | `.github/workflows/backend-ci.yml` — build and test on backend changes only |
| Tests | 1 (`HealthControllerTest`) |

### 1.3 The contract mismatch

The Android app is already compiled against four backend endpoints that do not exist. Every one of these code paths currently fails at runtime in any build with a configured backend.

| Endpoint the app calls | `BuildConfig` field | Backend implementation |
| --- | --- | --- |
| `/v1/mobile/concierge` | `CONCIERGE_ENDPOINT_PATH` | Missing |
| `/v1/mobile/notifications/email` | `NOTIFICATION_ENDPOINT_PATH` | Missing |
| `/v1/user/export` | `EXPORT_ENDPOINT_PATH` | Missing |
| `/v1/user/delete` | `DELETE_ENDPOINT_PATH` | Missing |

The last two are a compliance exposure, not merely a feature gap: the privacy centre presents working data-export and data-deletion controls to users, and those controls silently do nothing.

---

## 2. Target product shape

The product being built is **B2B2C**: rental agencies subscribe to the platform, and their customers rent vehicles through it.

```
Platform operator (us)
  └── Tenant: rental agency          ← pays us a subscription
        ├── Agency admins            ← manage fleet, approve bookings, review KYC
        └── Renters                  ← pay the agency per rental
```

Two distinct money flows exist and both need building:

1. **Tenant → platform.** Recurring subscription. Does not exist in any form today.
2. **Renter → tenant.** Per-rental payment. Today this is settled out of band; the renter uploads a photo of a payment receipt and an admin eyeballs it.

Three surfaces are needed at maturity:

- **Renter mobile app** — exists.
- **Agency web dashboard** — does not exist. Agency owners will not run a fleet from a phone.
- **Platform operator console** — does not exist. Needed for tenant lifecycle, revenue, and support.

---

## 3. Gap analysis

Ordered by how much each blocks commercial launch.

### 3.1 No billing — the SaaS-defining gap

Nothing in the codebase models money changing hands at the platform level. There is no payment provider integration, no `Plan`, `Subscription`, or `Invoice` concept, no plan tiers, no per-tenant quotas, no trial period, no dunning on failed payment, and no VAT or invoicing.

At the rental level, `dialog_payment_success.xml` is a confirmation dialog with no payment processor behind it. Settlement is a manual payment-proof upload reviewed by an agency admin.

### 3.2 Backend is effectively absent

Beyond the four missing endpoints above, there is no persistence layer, no domain service, and no server-authoritative business logic. Everything of consequence is decided on the client.

### 3.3 Business logic is client-side and therefore untrusted

Pricing (`PricingService`) and availability both run on the device. `firestore.rules` validates tenancy, role, verification status, and that `startDate < endDate`, but it does **not** validate the booking amount and does **not** detect overlapping bookings for the same vehicle. A modified client can book any car at any price, and two renters can hold the same vehicle for the same dates.

### 3.4 No tenant self-service onboarding

`RegisterCompanyActivity` writes a company document with `status: "pending_review"` and the flow ends there. There is no approval workflow, no notification to anyone, and no provisioning step. Activating a tenant means editing a Firestore field by hand in the console. A SaaS must take an agency from signup through trial to a provisioned, billed account with no operator involvement.

### 3.5 No platform operator console

There is no cross-tenant view of any kind: no tenant list, no MRR or churn, no ability to suspend a delinquent agency, no support impersonation. `firestore.rules` has no platform-superadmin role — the highest privilege that exists is `admin` scoped to a single company.

### 3.6 Android-only

No web presence at all. Agency staff need a desktop dashboard; renters without the app cannot discover or book anything.

### 3.7 Data layer will not support SaaS operations

Client-direct Firestore is a reasonable fit for the app itself, but cross-tenant analytics, revenue reporting, and billing reconciliation cannot be built on it. The backend README already anticipates Postgres; no work has started.

### 3.8 Production readiness

- No release signing configuration — the app cannot be published to Play.
- No Android CI. The existing workflow covers `backend/**` only.
- No staging or production environment, no infrastructure-as-code, no secret manager, and no deploy target for the existing `Dockerfile`.
- No observability: no crash reporting, no log aggregation, no metrics, no alerting.
- No Firestore backup or restore procedure, no disaster-recovery plan.
- No rate limiting or abuse protection on the backend.

### 3.9 Repository hygiene

Tracked in git but should not be: twelve `build_*.log` / `build_*.txt` artefacts, `trace*.txt`, a 4.6 MB `.logcat` capture, `.gradle/` directories under both root and `backend/`, and `app/google-services.json`.

### 3.10 Legal and commercial

- `PRIVACY_POLICY_URL` and `TOS_URL` default to `https://example.com/legal/*`.
- No terms of service for subscribing agencies, and no data processing agreement. Under GDPR the platform is a processor acting for each agency, and needs a DPA per tenant.
- Facial recognition is in scope. Face embeddings are biometric data used for identification, which is GDPR Article 9 special-category data. This requires an explicit lawful basis and a Data Protection Impact Assessment before commercial launch.

---

## 4. Delivery phases

Estimates assume one full-time developer.

### Phase 1 — Make the backend real (6–8 weeks)

The highest-risk phase, and everything else depends on it.

**Persistence**
- Add Spring Data JPA and PostgreSQL. Flyway for migrations.
- Entities: `Company`, `User` (mirror of the Firebase identity, keyed by uid), `Car`, `RentalRequest`, `VerificationRequest`.
- Decide and document the Firestore/Postgres split — see [Open decisions](#6-open-decisions).

**Move business logic server-side**
- Server-authoritative pricing. The client may display a quote; the server recomputes and is the source of truth on write.
- Availability and overlap checking, enforced with a database constraint or an exclusion index rather than application-level checks alone.
- Rental request state machine (`pending → approved → active → completed | cancelled | rejected`) enforced in one place.

**Close the endpoint contract**
- `POST /v1/mobile/concierge` — proxy to Gemini with the API key held server-side.
- `POST /v1/mobile/notifications/email` — transactional email via a provider (Postmark, SES, or similar).
- `POST /v1/user/export` — assemble and deliver a GDPR data export.
- `POST /v1/user/delete` — GDPR erasure, including Firebase Auth, Firestore documents, Storage objects, and biometric templates.

**Extend `/v1/me`** to return tenant, role, and subscription state so the client stops deriving authorization from raw documents.

**Exit criteria:** no privacy-centre control is a no-op; a booking cannot be created at a client-supplied price; overlapping bookings are impossible.

### Phase 2 — Billing (4–6 weeks)

- Select a payment provider. Stripe if the customer base is international; CMI or a local Moroccan acquirer if agencies are domestic — see [Open decisions](#6-open-decisions).
- Entities: `Plan`, `Subscription`, `Invoice`, `PaymentMethod`.
- Plan tiers with enforced quotas — vehicles per tenant, admin seats per tenant, bookings per month.
- Free trial with an automatic transition to paid or to suspended.
- Provider webhook handling: payment succeeded, payment failed, subscription cancelled.
- Dunning: retry schedule, notification emails, then suspension driving `CompanyLifecycleStatus`.
- Quota enforcement wired into the write paths added in Phase 1.
- VAT handling and invoice generation appropriate to the operating jurisdiction.

**Exit criteria:** an agency can subscribe, be charged on a recurring basis, and be automatically suspended for non-payment, with no manual intervention.

### Phase 3 — Self-service onboarding and operator console (4–5 weeks)

- Public signup: agency registers, verifies email, starts a trial, and is provisioned automatically.
- Tenant provisioning as a single transactional backend operation.
- Platform superadmin role, added to both the backend authorization model and `firestore.rules`.
- Operator console (web): tenant list and status, MRR and churn, manual suspend and reinstate, support impersonation with an audit trail.

**Exit criteria:** a new agency reaches a working, billed account without anyone on the platform side touching a console.

### Phase 4 — Agency web dashboard (5–7 weeks)

- Web front end against the Phase 1 API. Fleet management, booking approval, KYC review queue, reporting, and billing self-service.
- Optionally, a public per-tenant booking page so renters can book without installing the app.

### Phase 5 — Production hardening (3–4 weeks, partly parallelisable)

- Android release signing configuration and a Play Store listing.
- Android CI: assemble, lint, and unit tests on every pull request.
- Infrastructure as code; separate staging and production environments; secrets in a managed secret store.
- Deploy the backend container to a real target with health checks and rolling deploys.
- Crash reporting (Crashlytics or Sentry), structured logging, metrics, and alerting.
- Automated Firestore and Postgres backups with a rehearsed restore.
- Backend rate limiting and abuse protection.
- Replace placeholder legal URLs; publish a real privacy policy, terms of service, agency terms, and DPA template.
- Complete the DPIA covering biometric processing.
- Repository cleanup per [3.9](#39-repository-hygiene).

### Timeline summary

| Phase | Duration | Cumulative |
| --- | --- | --- |
| 1 — Backend | 6–8 weeks | 6–8 weeks |
| 2 — Billing | 4–6 weeks | 10–14 weeks |
| 3 — Onboarding + operator console | 4–5 weeks | 14–19 weeks |
| 4 — Agency web dashboard | 5–7 weeks | 19–26 weeks |
| 5 — Hardening | 3–4 weeks | 22–30 weeks |

Phases 1 and 2 are the minimum for "an agency can sign up and pay us without our involvement": roughly 10–14 weeks.

---

## 5. Cross-cutting concerns

### 5.1 Tenant isolation

Isolation is currently enforced in exactly one place — `firestore.rules`. Once the backend holds data, that guarantee must be re-established server-side: every query filtered by tenant, ideally enforced structurally (a mandatory tenant predicate or Postgres row-level security) rather than by developer discipline. Cross-tenant data leakage is the failure mode that ends a B2B SaaS.

### 5.2 Biometric data

The face-matching pipeline runs on-device today, which is the right default. Anything moved server-side raises the compliance burden sharply. Retention limits, deletion on account erasure, and the DPIA are prerequisites for launch, not follow-ups.

### 5.3 Migration strategy

The Android app is live against Firestore. Phase 1 must not require a big-bang cutover. Route new writes through the backend first, keep reads on Firestore until each collection has a backend equivalent, and migrate collection by collection.

Two constraints the mirror has to respect, both established by the V1 schema:

- **Companies sync before their users.** `app_user.company_id` carries a foreign key to `company`, so a user referencing an unmirrored company is rejected outright. Ordering the backfill company-first satisfies this; the alternative — dropping the constraint — would trade a trivial ordering requirement for silent referential corruption.
- **Unprovisioned callers are a normal state, not an error.** Until the backfill completes most users have no mirrored row. `GET /v1/me` reports `provisioned: false` with least-privilege defaults rather than failing, so the app keeps working on its existing Firestore path throughout the migration.

### 5.4 Testing

Current coverage is 8 unit tests on the client and 1 on the backend. Backend work should arrive with tests from the start — integration tests over a real Postgres (Testcontainers) for tenant isolation, pricing, and booking overlap in particular.

---

## 6. Open decisions

These block work in the phases noted and should be resolved before that phase starts.

| # | Decision | Blocks | Notes |
| --- | --- | --- | --- |
| 1 | Firestore/Postgres split — full migration, or Postgres for billing and analytics while operational data stays in Firestore | Phase 1 | Full migration costs more up front and removes the real-time client sync the app relies on. Recommend a hybrid: Postgres owns billing, tenants, and reporting; Firestore stays the operational read model. |
| 2 | Payment provider | Phase 2 | Stripe for international reach and far better developer experience; CMI or a local acquirer if the agencies are Moroccan and need domestic card rails. May need both. |
| 3 | Pricing model | Phase 2 | Per-seat, per-vehicle, flat tiers, or a commission on rental value. Per-vehicle tiers align price to tenant size and are simple to enforce as a quota. |
| 4 | Web stack | Phases 3–4 | Operator console and agency dashboard should share one stack. |
| 5 | Rental payment ownership | Phase 2 | Whether the platform processes renter payments (marketplace, higher compliance burden, new revenue line) or leaves them between agency and renter as today. |
| 6 | Namespace rename | Phase 5 | `com.example.carrentingtest` must become a real owned domain before Play Store submission. `applicationId` is already overridable; the Kotlin/Java package namespace is not yet renamed. |

Each of these should get its own ADR under `docs/decisions/` when resolved, following the pattern of `0001-backend-language-and-framework.md`.

---

## 7. Definition of done

The product is a running SaaS when all of the following hold:

- [ ] An agency can sign up, trial, subscribe, and be charged with no operator involvement.
- [ ] A delinquent agency is suspended automatically and reinstated on payment.
- [ ] No pricing, availability, or authorization decision is trusted from the client.
- [ ] Tenant isolation is enforced server-side and covered by tests.
- [ ] GDPR export and deletion actually execute end to end.
- [ ] The platform operator can see and manage every tenant from a console.
- [ ] Agency staff can do their whole job from a browser.
- [ ] The app is signed, published, and covered by CI.
- [ ] Errors are reported, metrics are collected, and alerts fire.
- [ ] Backups exist and a restore has been rehearsed.
- [ ] Privacy policy, terms of service, agency terms, and DPA are real documents at real URLs.
- [ ] The DPIA covering biometric processing is complete.

---

## Related documents

- [`decisions/0001-backend-language-and-framework.md`](decisions/0001-backend-language-and-framework.md) — why Spring Boot and Java 17.
- [`../backend/README.md`](../backend/README.md) — backend setup and current Phase 0 scope.
- [`../SECURITY.md`](../SECURITY.md) — security policy.
