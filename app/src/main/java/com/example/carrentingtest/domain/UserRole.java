package com.example.carrentingtest.domain;

import androidx.annotation.Nullable;

import java.util.Locale;

public enum UserRole {
    ADMIN,
    CLIENT,
    UNKNOWN;

    public static UserRole from(@Nullable String rawValue) {
        if (rawValue == null) {
            return UNKNOWN;
        }

        String normalized = rawValue.trim().toLowerCase(Locale.US);
        if ("admin".equals(normalized)) {
            return ADMIN;
        }
        if ("client".equals(normalized)) {
            return CLIENT;
        }
        return UNKNOWN;
    }
}
