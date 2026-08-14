package com.bbluxurycars.backend.web.dto;

import jakarta.validation.constraints.NotBlank;

/** Body of {@code POST /v1/mobile/concierge}. */
public record ConciergeRequest(
        @NotBlank String query,
        String inventoryContext) {
}
