-- Tenant foundation: the `company` and `app_user` tables.
--
-- These mirror the Firestore `companies` and `users` collections. Per open
-- decision 1 in docs/SAAS_ROADMAP.md the split is hybrid: Firestore stays the
-- operational store the Android client reads and writes live (keeping its
-- real-time sync and requiring no client rewrite), while Postgres becomes the
-- system of record for tenancy, billing and reporting.
--
-- Primary keys are the Firestore identifiers themselves -- the company document
-- id, and the Firebase Auth uid -- rather than generated surrogates. A mirrored
-- row can then always be matched back to its Firestore counterpart directly,
-- with no correlation table and no second round trip during sync.
--
-- Status columns store the same lowercase strings Firestore holds, so the
-- mirror is lossless in both directions. The CHECK constraints below are the
-- server-side twin of the enums in the Android app
-- (app/src/main/java/com/example/carrentingtest/domain/).

CREATE TABLE company (
    id                 VARCHAR(128)  PRIMARY KEY,
    name               VARCHAR(255)  NOT NULL,
    phone              VARCHAR(64),
    address            VARCHAR(512),
    location_latitude  DOUBLE PRECISION,
    location_longitude DOUBLE PRECISION,
    status             VARCHAR(32)   NOT NULL DEFAULT 'pending_review',
    created_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT company_status_check
        CHECK (status IN ('pending_review', 'approved', 'suspended', 'rejected')),

    -- A coordinate is stored as a complete pair or not at all. Half a
    -- coordinate is silently wrong on a map rather than visibly absent, so it
    -- is rejected at the schema level instead of being defended against in
    -- every consumer.
    CONSTRAINT company_location_complete
        CHECK ((location_latitude IS NULL) = (location_longitude IS NULL)),
    CONSTRAINT company_latitude_range
        CHECK (location_latitude IS NULL OR location_latitude BETWEEN -90 AND 90),
    CONSTRAINT company_longitude_range
        CHECK (location_longitude IS NULL OR location_longitude BETWEEN -180 AND 180)
);

CREATE TABLE app_user (
    id                  VARCHAR(128) PRIMARY KEY,
    company_id          VARCHAR(128) REFERENCES company (id),
    email               VARCHAR(320),
    full_name           VARCHAR(255),
    phone               VARCHAR(64),
    role                VARCHAR(32)  NOT NULL DEFAULT 'unknown',
    status              VARCHAR(64)  NOT NULL DEFAULT 'active',
    verification_status VARCHAR(32)  NOT NULL DEFAULT 'not_started',
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT app_user_role_check
        CHECK (role IN ('admin', 'client', 'unknown')),
    CONSTRAINT app_user_status_check
        CHECK (status IN ('active', 'pending_company_approval', 'suspended')),
    CONSTRAINT app_user_verification_status_check
        CHECK (verification_status IN
               ('not_started', 'submitted', 'under_review', 'approved', 'rejected'))
);

-- company_id is nullable because a user exists in Firebase Auth from the moment
-- they sign up, before any company is linked. It is left without ON DELETE
-- behaviour deliberately: companies are never deleted (firestore.rules denies
-- it outright), so the default RESTRICT turns any attempt into a loud error
-- rather than a silent cascade through tenant data.

-- Every tenant-scoped read filters on company_id, so it carries an index from
-- the start rather than waiting for the first slow query in production.
CREATE INDEX idx_app_user_company_id ON app_user (company_id);

-- Supports admin lookups by email. Not unique: Firebase Auth already enforces
-- uniqueness upstream, and a mirror that lags reality should not reject a sync
-- write for a constraint it does not own.
CREATE INDEX idx_app_user_email ON app_user (email);
