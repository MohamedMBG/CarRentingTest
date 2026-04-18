package com.example.carrentingtest.domain;

import androidx.annotation.Nullable;

import java.util.Locale;

public enum RentalRequestStatus {
    PENDING("pending"),
    APPROVED("approved"),
    REJECTED("rejected"),
    COMPLETED("completed");

    private final String storageValue;

    RentalRequestStatus(String storageValue) {
        this.storageValue = storageValue;
    }

    public String getStorageValue() {
        return storageValue;
    }

    public static RentalRequestStatus from(@Nullable String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return PENDING;
        }

        String normalized = rawValue.trim().toLowerCase(Locale.US);
        switch (normalized) {
            case "approved":
            case "ongoing":
                return APPROVED;
            case "rejected":
                return REJECTED;
            case "completed":
                return COMPLETED;
            case "pending":
            default:
                return PENDING;
        }
    }

    public boolean isRevenueRecognized() {
        return this == APPROVED || this == COMPLETED;
    }
}
