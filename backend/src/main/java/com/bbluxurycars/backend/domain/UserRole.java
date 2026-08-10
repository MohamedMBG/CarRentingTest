package com.bbluxurycars.backend.domain;

import java.util.Locale;

/**
 * Server-side twin of the Android app's {@code UserRole}. Parsing is
 * deliberately as permissive as the client's: the mirror must be able to
 * ingest whatever Firestore actually contains, including documents written by
 * older app versions, without failing the sync.
 */
public enum UserRole implements StoredEnum {

    ADMIN("admin"),
    CLIENT("client"),
    UNKNOWN("unknown");

    private final String storageValue;

    UserRole(String storageValue) {
        this.storageValue = storageValue;
    }

    @Override
    public String getStorageValue() {
        return storageValue;
    }

    public static UserRole from(String rawValue) {
        if (rawValue == null) {
            return UNKNOWN;
        }
        String normalized = rawValue.trim().toLowerCase(Locale.US);
        return switch (normalized) {
            case "admin" -> ADMIN;
            case "client" -> CLIENT;
            default -> UNKNOWN;
        };
    }
}
