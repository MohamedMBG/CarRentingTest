package com.bbluxurycars.backend.web.dto;

import com.bbluxurycars.backend.domain.RentalRequest;

import java.time.Instant;

/**
 * Wire shape of a booking.
 *
 * <p>{@code status} goes out as the lowercase string Firestore already holds,
 * so the Android client feeds it straight into its existing
 * {@code RentalRequestStatus.from(...)}.
 *
 * <p>The quote is included in full rather than as a bare total: the renter is
 * entitled to see how the figure was reached, and support cannot reconstruct a
 * total from a number alone.
 */
public record BookingResponse(
        String id,
        String carId,
        String userId,
        Instant startAt,
        Instant endAt,
        String status,
        String additionalRequests,
        QuoteResponse pricing,
        Instant completedAt,
        Instant createdAt) {

    public static BookingResponse from(RentalRequest request) {
        return new BookingResponse(
                request.getId(),
                request.getCarId(),
                request.getUserId(),
                request.getStartAt(),
                request.getEndAt(),
                request.getStatus().getStorageValue(),
                request.getAdditionalRequests(),
                QuoteResponse.from(request.getPricing()),
                request.getCompletedAt(),
                request.getCreatedAt());
    }
}
