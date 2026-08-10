package com.bbluxurycars.backend.domain;

import java.util.Locale;

/**
 * Server-side twin of the Android app's {@code CompanyLifecycleStatus}.
 *
 * <p>This is the status that Phase 2 billing will drive: a tenant that stops
 * paying moves to {@link #SUSPENDED} (docs/SAAS_ROADMAP.md).
 */
public enum CompanyLifecycleStatus implements StoredEnum {

    PENDING_REVIEW("pending_review"),
    APPROVED("approved"),
    SUSPENDED("suspended"),
    REJECTED("rejected");

    private final String storageValue;

    CompanyLifecycleStatus(String storageValue) {
        this.storageValue = storageValue;
    }

    @Override
    public String getStorageValue() {
        return storageValue;
    }

    /**
     * Whether the tenant may currently transact. Kept here rather than at each
     * call site so that Phase 2 can add billing-driven states without hunting
     * down scattered status comparisons.
     */
    public boolean isOperational() {
        return this == APPROVED;
    }

    /**
     * Unrecognised and absent values fall back to {@link #PENDING_REVIEW},
     * matching the client. Defaulting an unknown status to the least
     * privileged state means a malformed document cannot accidentally grant a
     * tenant access it was never approved for.
     */
    public static CompanyLifecycleStatus from(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return PENDING_REVIEW;
        }
        String normalized = rawValue.trim().toLowerCase(Locale.US);
        return switch (normalized) {
            case "approved", "active" -> APPROVED;
            case "suspended" -> SUSPENDED;
            case "rejected" -> REJECTED;
            default -> PENDING_REVIEW;
        };
    }
}
