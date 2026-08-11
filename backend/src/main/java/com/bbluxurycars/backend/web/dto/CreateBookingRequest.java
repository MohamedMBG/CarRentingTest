package com.bbluxurycars.backend.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * Body of {@code POST /v1/bookings}.
 *
 * <p>Like {@link QuoteRequest} it has no price and no tenant: both are derived
 * server-side from the car row and the verified caller. The renter's identity
 * is likewise absent -- a booking is always created for the authenticated
 * caller, so there is no field with which to book on someone else's behalf.
 */
public record CreateBookingRequest(
        @NotBlank String carId,
        @NotNull Instant startAt,
        @NotNull Instant endAt,
        @Size(max = 2000) String additionalRequests) {
}
