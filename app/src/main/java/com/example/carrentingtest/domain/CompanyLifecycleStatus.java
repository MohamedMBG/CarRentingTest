package com.example.carrentingtest.domain;

import androidx.annotation.Nullable;

import java.util.Locale;

public enum CompanyLifecycleStatus {
    PENDING_REVIEW("pending_review"),
    APPROVED("approved"),
    SUSPENDED("suspended"),
    REJECTED("rejected");

    private final String storageValue;

    CompanyLifecycleStatus(String storageValue) {
        this.storageValue = storageValue;
    }

    public String getStorageValue() {
        return storageValue;
    }

    public static CompanyLifecycleStatus from(@Nullable String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return PENDING_REVIEW;
        }

        String normalized = rawValue.trim().toLowerCase(Locale.US);
        switch (normalized) {
            case "approved":
            case "active":
                return APPROVED;
            case "suspended":
                return SUSPENDED;
            case "rejected":
                return REJECTED;
            case "pending_review":
            default:
                return PENDING_REVIEW;
        }
    }
}
