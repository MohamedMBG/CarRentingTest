-- Fleet and bookings: the `car` and `rental_request` tables.
--
-- These are the first tables the backend writes to as the authority rather than
-- as a mirror. Today pricing and availability are decided on the Android device
-- (PricingService, and no overlap check anywhere), which means a modified
-- client can book any car at any price and two renters can hold the same
-- vehicle for the same dates -- gap 3.3 in docs/SAAS_ROADMAP.md. The schema
-- below is where that stops being possible.
--
-- Identifiers stay VARCHAR(128) holding Firestore document ids, matching V1, so
-- a row created here can be mirrored back into Firestore under the same id and
-- rows already in Firestore can be backfilled without a correlation table.

-- Required by the exclusion constraint at the bottom of this file: a GiST index
-- cannot combine an equality test on a scalar column (car_id) with a range
-- overlap test without the operator classes this extension supplies.
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- Lets child tables carry a composite foreign key on (id, company_id). A plain
-- FK to app_user(id) would prove the renter exists but not that they belong to
-- the booking's tenant; the composite version makes a cross-tenant reference
-- unrepresentable rather than merely discouraged.
ALTER TABLE app_user
    ADD CONSTRAINT app_user_id_company_unique UNIQUE (id, company_id);

CREATE TABLE car (
    id                VARCHAR(128)   PRIMARY KEY,
    company_id        VARCHAR(128)   NOT NULL REFERENCES company (id),
    model             VARCHAR(255)   NOT NULL,
    type              VARCHAR(64),
    price_per_day     NUMERIC(12, 2) NOT NULL,
    seats             INTEGER,
    transmission_type VARCHAR(32),
    image_url         VARCHAR(1024),
    available         BOOLEAN        NOT NULL DEFAULT TRUE,
    maintenance       BOOLEAN        NOT NULL DEFAULT FALSE,
    rental_count      INTEGER        NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),

    -- NUMERIC, not DOUBLE PRECISION: this column is money and is multiplied by
    -- a day count to produce a total the renter is charged. Binary floating
    -- point cannot represent 0.01 exactly, so totals would drift by cents in a
    -- way that never reconciles against Phase 2 invoices.
    CONSTRAINT car_price_per_day_non_negative CHECK (price_per_day >= 0),
    CONSTRAINT car_seats_positive CHECK (seats IS NULL OR seats > 0),

    -- Target of the composite foreign key from rental_request below.
    CONSTRAINT car_id_company_unique UNIQUE (id, company_id)
);

-- Every fleet read is "the cars of one tenant", so the tenant predicate is
-- indexed from the start.
CREATE INDEX idx_car_company_id ON car (company_id);

CREATE TABLE rental_request (
    id                  VARCHAR(128)   PRIMARY KEY,
    company_id          VARCHAR(128)   NOT NULL REFERENCES company (id),
    car_id              VARCHAR(128)   NOT NULL,
    user_id             VARCHAR(128)   NOT NULL,
    start_at            TIMESTAMPTZ    NOT NULL,
    end_at              TIMESTAMPTZ    NOT NULL,
    status              VARCHAR(32)    NOT NULL DEFAULT 'pending',
    additional_requests VARCHAR(2000),

    -- The priced quote, recomputed by the server on write and stored as the
    -- evidence behind total_price. Kept as columns rather than a JSON blob so
    -- Phase 2 revenue reporting can aggregate them in SQL.
    currency            VARCHAR(3)     NOT NULL DEFAULT 'MAD',
    unit_price_per_day  NUMERIC(12, 2) NOT NULL,
    rental_days         INTEGER        NOT NULL,
    base_price          NUMERIC(12, 2) NOT NULL,
    extras_total        NUMERIC(12, 2) NOT NULL DEFAULT 0,
    discount_total      NUMERIC(12, 2) NOT NULL DEFAULT 0,
    total_price         NUMERIC(12, 2) NOT NULL,

    completed_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ    NOT NULL DEFAULT now(),

    -- The car and the renter must both belong to the booking's tenant. Stated
    -- as composite foreign keys so that isolation holds even if some future
    -- code path forgets to filter -- structural enforcement rather than
    -- developer discipline (docs/SAAS_ROADMAP.md 5.1).
    CONSTRAINT rental_request_car_same_tenant
        FOREIGN KEY (car_id, company_id) REFERENCES car (id, company_id),
    CONSTRAINT rental_request_user_same_tenant
        FOREIGN KEY (user_id, company_id) REFERENCES app_user (id, company_id),

    CONSTRAINT rental_request_status_check
        CHECK (status IN ('pending', 'approved', 'rejected', 'completed')),
    CONSTRAINT rental_request_period_ordered
        CHECK (start_at < end_at),
    CONSTRAINT rental_request_rental_days_positive
        CHECK (rental_days > 0),
    CONSTRAINT rental_request_amounts_non_negative
        CHECK (unit_price_per_day >= 0
               AND base_price >= 0
               AND extras_total >= 0
               AND discount_total >= 0
               AND total_price >= 0),

    -- completed_at exists exactly when the booking is completed, so "is this
    -- finished?" has one answer rather than two that can disagree.
    CONSTRAINT rental_request_completed_at_matches_status
        CHECK ((status = 'completed') = (completed_at IS NOT NULL))
);

CREATE INDEX idx_rental_request_company_id ON rental_request (company_id);

-- Renters read their own bookings; agency admins read the tenant's queue.
CREATE INDEX idx_rental_request_user_id ON rental_request (company_id, user_id);

-- THE overlap guarantee.
--
-- Enforced as an exclusion constraint rather than a read-then-write check in
-- application code: two concurrent approvals both read "no conflict" before
-- either writes, so an application-level check cannot close the race without
-- serialising the whole table. Postgres evaluates this at write time under the
-- index's own locking, so the second writer fails no matter the interleaving.
--
-- tstzrange defaults to '[)' -- inclusive start, exclusive end -- so a booking
-- ending exactly when the next begins does not overlap. Back-to-back rentals of
-- the same vehicle stay legal, which is the normal case for a busy fleet.
--
-- Scoped to statuses that actually hold the vehicle. 'pending' is excluded on
-- purpose: several renters may request the same car for the same dates and the
-- agency admin chooses between them, which is how the app works today. The
-- constraint bites at approval instead, where the second approval is the one
-- that fails. 'completed' is included so a past rental still blocks a
-- retroactively created overlapping one.
ALTER TABLE rental_request
    ADD CONSTRAINT rental_request_no_overlapping_hold
    EXCLUDE USING gist (
        car_id WITH =,
        tstzrange(start_at, end_at) WITH &&
    ) WHERE (status IN ('approved', 'completed'));
