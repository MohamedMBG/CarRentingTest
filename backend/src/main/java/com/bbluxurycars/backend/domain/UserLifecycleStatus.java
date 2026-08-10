package com.bbluxurycars.backend.domain;

import java.util.Locale;

/**
 * Server-side twin of the Android app's {@code UserLifecycleStatus}.
 */
public enum UserLifecycleStatus implements StoredEnum {

    ACTIVE("active"),
    PENDING_COMPANY_APPROVAL("pending_company_approval"),
    SUSPENDED("suspended");

    private final String storageValue;

    UserLifecycleStatus(String storageValue) {
        this.storageValue = storageValue;
    }

    @Override
    public String getStorageValue() {
        return storageValue;
    }

    /**
     * The role-dependent default matches the client exactly: a document with no
     * status means an admin still awaiting company approval, but an ordinary
     * client who is simply active. Diverging here would let the backend and the
     * app disagree about whether the same user may act.
     */
    public static UserLifecycleStatus from(String rawValue, UserRole role) {
        if (rawValue == null || rawValue.isBlank()) {
            return defaultFor(role);
        }
        String normalized = rawValue.trim().toLowerCase(Locale.US);
        return switch (normalized) {
            case "active", "approved" -> ACTIVE;
            case "pending_company_approval", "pending_review" -> PENDING_COMPANY_APPROVAL;
            case "suspended" -> SUSPENDED;
            default -> defaultFor(role);
        };
    }

    private static UserLifecycleStatus defaultFor(UserRole role) {
        return role == UserRole.ADMIN ? PENDING_COMPANY_APPROVAL : ACTIVE;
    }
}
