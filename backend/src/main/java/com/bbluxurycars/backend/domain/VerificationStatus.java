package com.bbluxurycars.backend.domain;

import java.util.Locale;

/**
 * Server-side twin of the Android app's {@code VerificationStatus} (KYC).
 *
 * <p>Only the status is mirrored into Postgres. The evidence behind it -- the
 * licence image, the selfie and the face embedding -- stays where it is and is
 * never copied here: it is GDPR Article 9 biometric data, and every additional
 * copy widens the erasure surface and the DPIA scope (docs/SAAS_ROADMAP.md).
 */
public enum VerificationStatus implements StoredEnum {

    NOT_STARTED("not_started"),
    SUBMITTED("submitted"),
    UNDER_REVIEW("under_review"),
    APPROVED("approved"),
    REJECTED("rejected");

    private final String storageValue;

    VerificationStatus(String storageValue) {
        this.storageValue = storageValue;
    }

    @Override
    public String getStorageValue() {
        return storageValue;
    }

    /**
     * Server-side counterpart of the client's booking gate. When booking moves
     * behind the API this becomes the authoritative check; today it exists so
     * both sides answer the question identically.
     */
    public boolean allowsBooking() {
        return this == APPROVED;
    }

    public static VerificationStatus from(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return NOT_STARTED;
        }
        String normalized = rawValue.trim().toLowerCase(Locale.US);
        return switch (normalized) {
            case "approved", "verified" -> APPROVED;
            case "rejected", "failed" -> REJECTED;
            case "under_review", "pending" -> UNDER_REVIEW;
            case "submitted" -> SUBMITTED;
            default -> NOT_STARTED;
        };
    }
}
