package com.example.carrentingtest.domain;

import androidx.annotation.Nullable;

import java.util.Locale;

public enum UserLifecycleStatus {
    ACTIVE("active"),
    PENDING_COMPANY_APPROVAL("pending_company_approval"),
    SUSPENDED("suspended");

    private final String storageValue;

    UserLifecycleStatus(String storageValue) {
        this.storageValue = storageValue;
    }

    public String getStorageValue() {
        return storageValue;
    }

    public static UserLifecycleStatus from(@Nullable String rawValue, @Nullable UserRole role) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return role == UserRole.ADMIN ? PENDING_COMPANY_APPROVAL : ACTIVE;
        }

        String normalized = rawValue.trim().toLowerCase(Locale.US);
        switch (normalized) {
            case "active":
                return ACTIVE;
            case "pending_company_approval":
            case "pending_review":
                return PENDING_COMPANY_APPROVAL;
            case "suspended":
                return SUSPENDED;
            default:
                return role == UserRole.ADMIN ? PENDING_COMPANY_APPROVAL : ACTIVE;
        }
    }
}
