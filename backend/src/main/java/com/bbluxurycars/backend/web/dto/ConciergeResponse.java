package com.bbluxurycars.backend.web.dto;

/**
 * Response of {@code POST /v1/mobile/concierge}.
 *
 * <p>{@code recommendation} is the exact field name {@code GeminiHelper} on
 * the Android side already reads.
 */
public record ConciergeResponse(String recommendation) {
}
