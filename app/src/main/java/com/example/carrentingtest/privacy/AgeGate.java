package com.example.carrentingtest.privacy;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Age gate utility. Rental contracts require the user to be of legal driving / contract age;
 * we enforce 18+ as the minimum across all supported regions. DOB stored ISO-8601
 * (yyyy-MM-dd) on the user document.
 */
public final class AgeGate {

    public static final int MINIMUM_AGE_YEARS = 18;
    public static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;

    private AgeGate() {}

    @Nullable
    public static LocalDate parseIsoDob(@Nullable String iso) {
        if (iso == null || iso.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(iso, ISO);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public static boolean isAtLeastMinimumAge(@NonNull LocalDate dob) {
        return isAtLeastMinimumAge(dob, LocalDate.now());
    }

    /**
     * Today-injectable variant for unit tests. {@code today} is the reference point
     * against which the user's age is computed.
     */
    public static boolean isAtLeastMinimumAge(@NonNull LocalDate dob, @NonNull LocalDate today) {
        if (dob.isAfter(today)) {
            return false;
        }
        return Period.between(dob, today).getYears() >= MINIMUM_AGE_YEARS;
    }
}
