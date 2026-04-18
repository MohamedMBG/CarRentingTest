package com.example.carrentingtest.verification;

import androidx.annotation.Nullable;

import java.util.Locale;

public enum VerificationStatus {
    NOT_STARTED("not_started"),
    SUBMITTED("submitted"),
    UNDER_REVIEW("under_review"),
    APPROVED("approved"),
    REJECTED("rejected");

    private final String storageValue;

    VerificationStatus(String storageValue) {
        this.storageValue = storageValue;
    }

    public String getStorageValue() {
        return storageValue;
    }

    public boolean allowsBooking() {
        return this == APPROVED;
    }

    public static VerificationStatus from(@Nullable String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return NOT_STARTED;
        }

        String normalized = rawValue.trim().toLowerCase(Locale.US);
        switch (normalized) {
            case "approved":
            case "verified":
                return APPROVED;
            case "rejected":
            case "failed":
                return REJECTED;
            case "under_review":
            case "pending":
                return UNDER_REVIEW;
            case "submitted":
                return SUBMITTED;
            case "unverified":
            case "not_started":
            default:
                return NOT_STARTED;
        }
    }
}
