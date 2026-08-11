package com.bbluxurycars.backend.domain;

import java.util.Locale;

/**
 * Server-side twin of the Android app's {@code RentalRequestStatus}, plus the
 * state machine the app never had.
 *
 * <p>The client stores a status string and each screen decides for itself which
 * transitions are offered, so nothing prevents a stale screen from rejecting an
 * already-completed booking. {@link #canTransitionTo(RentalRequestStatus)}
 * makes the legal moves a single fact the server enforces on every write.
 */
public enum RentalRequestStatus implements StoredEnum {

    PENDING("pending"),
    APPROVED("approved"),
    REJECTED("rejected"),
    COMPLETED("completed");

    private final String storageValue;

    RentalRequestStatus(String storageValue) {
        this.storageValue = storageValue;
    }

    @Override
    public String getStorageValue() {
        return storageValue;
    }

    /**
     * {@code pending → approved | rejected}, {@code approved → completed}, and
     * nothing else. Rejected and completed are terminal: money and vehicle
     * availability have already been decided on their basis, so reopening them
     * would silently invalidate both.
     */
    public boolean canTransitionTo(RentalRequestStatus target) {
        if (target == null || target == this) {
            return false;
        }
        return switch (this) {
            case PENDING -> target == APPROVED || target == REJECTED;
            case APPROVED -> target == COMPLETED;
            case REJECTED, COMPLETED -> false;
        };
    }

    /**
     * Whether a booking in this status holds the vehicle for its dates. Must
     * stay in step with the WHERE clause of {@code
     * rental_request_no_overlapping_hold} in V2: the database is the authority,
     * and this method exists so callers can predict its answer without waiting
     * for a constraint violation.
     */
    public boolean holdsVehicle() {
        return this == APPROVED || this == COMPLETED;
    }

    public boolean isRevenueRecognized() {
        return this == APPROVED || this == COMPLETED;
    }

    /**
     * Parsing mirrors the client's, including its {@code ongoing} alias for
     * approved, so documents written by older app versions map to the same
     * constant on both sides.
     */
    public static RentalRequestStatus from(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return PENDING;
        }
        String normalized = rawValue.trim().toLowerCase(Locale.US);
        return switch (normalized) {
            case "approved", "ongoing" -> APPROVED;
            case "rejected" -> REJECTED;
            case "completed" -> COMPLETED;
            default -> PENDING;
        };
    }
}
