package com.bbluxurycars.backend.web.dto;

import com.bbluxurycars.backend.tenant.TenantContext;

/**
 * Wire shape of {@code GET /v1/me}.
 *
 * <p>Status fields are emitted as the same lowercase strings Firestore holds,
 * so the Android client can feed them straight into its existing
 * {@code UserRole.from(...)} / {@code CompanyLifecycleStatus.from(...)} parsers
 * and adopt this endpoint without a parallel set of constants.
 *
 * <p>{@code permissions} is derived server-side rather than left to the client
 * to recompute. The client currently answers "may this user book?" itself; once
 * the server states it, the two cannot drift apart.
 */
public record MeResponse(
        String uid,
        boolean provisioned,
        String companyId,
        String role,
        String userStatus,
        String companyStatus,
        String verificationStatus,
        Permissions permissions) {

    public record Permissions(boolean canBook, boolean isActiveAdmin) {
    }

    public static MeResponse from(TenantContext context) {
        return new MeResponse(
                context.uid(),
                context.provisioned(),
                context.companyId(),
                context.role().getStorageValue(),
                context.userStatus().getStorageValue(),
                context.companyStatus().getStorageValue(),
                context.verificationStatus().getStorageValue(),
                new Permissions(context.canBook(), context.isActiveAdmin()));
    }
}
