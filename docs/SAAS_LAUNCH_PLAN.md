# SaaS Launch Roadmap — CarRentingTest

Audit-derived plan for hosting and publishing CarRentingTest as a SaaS product.

Stack: Android Kotlin/Java + Firebase (Auth, Firestore, Storage, FCM, Analytics, ML Kit) + custom backend (HTTP). Multi-tenant data model present. Core rental + verification + admin flows shipped.

---

## P0 — Launch Blockers

### 1. Secrets hygiene
- `app/google-services.json` tracked in git → rotate Firebase API key, add to `.gitignore`, inject via CI secret.
- `applicationId` still `com.example.carrentingtest` → rename to real domain (e.g. `com.yourbrand.carrenting`). Touches manifests, Firebase project, signing.
- Audit `.env` / `local.properties`. Confirm `BACKEND_BASE_URL` only via gradle property.

### 2. Payment processing (HARD BLOCKER)
- Current `AdminPosActivity` = manual proof upload. Not SaaS-grade.
- Options:
  - **B2B SaaS (rental companies pay platform)**: Stripe Billing subscriptions per company. Backend + webhook → Firestore `companies/{id}/subscription`.
  - **B2C in-app (renter pays platform)**: Stripe Android SDK (`stripe-android`) or Google Pay. PaymentIntent flow via backend.
- Need: refund flow, server-side invoice PDF generation, tax handling (VAT/sales tax).

### 3. Legal / GDPR
- Privacy Policy + ToS screens (load remote HTML).
- Consent screen on first launch (analytics + marketing opt-in separate).
- Data export endpoint (`/v1/user/export`).
- Deletion flow (`/v1/user/delete` → cascade Firestore + Storage + Auth).
- Cookie/SDK disclosure (FCM, Analytics, Crashlytics).
- Age gate if needed (18+ for rentals).

### 4. Observability
- Add **Crashlytics**. Wire `FirebaseCrashlytics.setUserId` post-login.
- Add **Performance Monitoring**.
- Instrument Analytics events: `sign_up`, `verification_submitted`, `rental_requested`, `payment_completed`.
- Backend: structured logging + error tracking (Sentry).

---

## P1 — Production Hardening

### 5. Security
- Network security config XML: enforce TLS 1.2+, optionally cert-pin backend.
- Replace `HttpURLConnection` in `BackendClient` → OkHttp + Retrofit. Native cert pinning.
- Firebase App Check (attest device → block bot Firestore writes).
- Password policy: min 8 chars, mixed case + digit. Enforce in `SignUpActivity`.
- Email verification gate before rental.
- Re-audit Firestore/Storage rules adversarially (Firebase Emulator + rules unit tests).
- Rate-limit verification submission backend-side.

### 6. Backend infra
- Move polling worker (`NotificationCheckWorker` 15min) → push-only via FCM. Polling = battery + scale tax.
- Migrate critical workflows to **Cloud Functions** (or containerize custom backend, autoscale): rental approval, payment webhook, verification status change, notification fan-out.
- Idempotency keys on POST endpoints.
- Backup: Firestore scheduled exports → GCS bucket.

### 7. Testing
- Delete `ExampleUnitTest.java` boilerplate.
- Coverage target 60%+ on `data/`, `domain/`, repositories, `PricingService`, `AdminAccessManager`.
- Espresso flows: sign-up → verify → book → cancel.
- **Firebase Test Lab** matrix in CI.
- Rules tests (`@firebase/rules-unit-testing`) for Firestore + Storage.

### 8. CI/CD
- GitHub Actions:
  - PR: lint + unit tests + rules tests
  - main: assemble release AAB, upload Play Internal Track via Fastlane / `r0adkll/upload-google-play`
- Signing: keystore in GitHub secrets (base64). Never commit.
- Versioning: auto-bump `versionCode` from CI build number.

---

## P2 — SaaS Operational

### 9. Multi-tenant onboarding
- Self-serve company signup (`RegisterCompanyActivity` exists — verify approval workflow end-to-end).
- Super-admin panel (web, not mobile): approve/suspend companies, billing view, push announcements.
- Per-tenant feature flags via Remote Config (already integrated).
- Per-tenant quotas (cars count, requests/month) tied to subscription plan.

### 10. Web admin portal
- Mobile-only admin = bad SaaS ops. Build minimal Next.js/React admin reusing Firestore + backend.
- Scope: company approval, user management, refund issuance, dispute resolution, KPI dashboards.

### 11. Localization
- Audit AR/FR coverage. Fix RTL layouts for Arabic.
- Currency/locale formatting (`NumberFormat.getCurrencyInstance(locale)`).
- Date pickers respect locale calendar.

### 12. Accessibility
- TalkBack pass on all flows. Label every `ImageButton`.
- Contrast ratio audit (WCAG AA).
- Dynamic font scaling test.

### 13. Performance
- Lottie + TensorFlow Lite + MPAndroidChart + POI + Picasso + Glide = bloated. Pick one image loader (Glide), drop Picasso. Tree-shake POI (server-side report gen instead?).
- Measure APK/AAB size, target <40MB.
- Cold start profiling (Macrobenchmark).

---

## P3 — Polish

- Onboarding tutorial screens.
- In-app review prompt (Play Core).
- Push notification preferences screen.
- Referral system (if growth lever).
- Dark mode audit.
- Empty/error states for every list screen.

---

## Suggested Sequencing (12-week launch)

| Week | Track |
|------|-------|
| 1-2 | Secrets rotation, applicationId rename, Crashlytics, App Check |
| 3-4 | Payment integration (Stripe), webhook + invoice |
| 5 | Legal screens + GDPR delete/export |
| 6-7 | CI/CD + Espresso + rules tests |
| 8-9 | Web admin portal MVP |
| 10 | Backend → Cloud Functions migration, drop polling worker |
| 11 | Security review (external pentest), perf pass, a11y pass |
| 12 | Play Store internal → closed beta → production |

---

## Readiness Matrix

| Category | Status | Notes |
|----------|--------|-------|
| Tech Stack | OK | Kotlin/Java, ViewBinding, Firebase, custom backend |
| Features | OK | Auth, rentals, verification, admin, AI concierge |
| Security | Medium risk | google-services.json exposed; weak password validation; no cert pinning |
| Payment | **BLOCKER** | Proof-of-upload only; no real processing |
| Multi-Tenancy | OK | Company scoping in Firestore + tenant session |
| Testing | Poor | <10 unit tests; no instrumented; no crash reporting |
| Accessibility | Partial | Basic contentDescriptions; no TalkBack pass |
| Backend | Custom | No Cloud Functions; mobile-only admin; scaling concerns |
| CI/CD | None | Manual builds |
| Legal/Compliance | **MISSING** | No privacy policy, GDPR consent, deletion flow |
| Localization | Partial | AR/FR; English primary |
