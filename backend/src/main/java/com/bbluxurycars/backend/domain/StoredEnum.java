package com.bbluxurycars.backend.domain;

/**
 * Implemented by enums that persist as a fixed lowercase string rather than as
 * their Java constant name.
 *
 * <p>The stored values are the ones Firestore already holds, because Postgres
 * mirrors Firestore rather than replacing it (docs/SAAS_ROADMAP.md, open
 * decision 1). Persisting {@code name()} instead would make every sync step
 * translate between {@code PENDING_REVIEW} and {@code pending_review}, and any
 * missed translation would be a silent tenant-status mismatch.
 */
public interface StoredEnum {

    String getStorageValue();
}
