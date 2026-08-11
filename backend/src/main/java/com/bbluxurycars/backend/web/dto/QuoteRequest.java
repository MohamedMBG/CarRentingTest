package com.bbluxurycars.backend.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Body of {@code POST /v1/bookings/quote}.
 *
 * <p>Carries no price field. There is nothing for the client to propose: the
 * server prices from the car row, and accepting a number here would create the
 * very input that must not exist.
 */
public record QuoteRequest(
        @NotBlank String carId,
        @NotNull Instant startAt,
        @NotNull Instant endAt) {
}
