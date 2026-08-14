package com.bbluxurycars.backend.web.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Body of {@code POST /v1/user/export}: everything the backend holds about
 * the caller, assembled synchronously rather than as a queued job -- there is
 * no job/email infrastructure to hand this off to yet, and returning the data
 * directly is a working right-of-access response rather than a stub for one.
 *
 * <p>{@code account}/{@code firestoreProfile}/{@code firestoreVerification}
 * are {@code null} or empty when nothing is on file, which is itself
 * meaningful (e.g. a caller who never finished onboarding).
 */
public record UserExportResponse(
        String uid,
        Instant exportedAt,
        Map<String, Object> account,
        List<Map<String, Object>> bookings,
        Map<String, Object> firestoreProfile,
        Map<String, Object> firestoreVerification,
        List<Map<String, Object>> firestoreBookings) {
}
