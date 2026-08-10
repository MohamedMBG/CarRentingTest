package com.bbluxurycars.backend.tenant;

import com.bbluxurycars.backend.domain.CompanyLifecycleStatus;
import com.bbluxurycars.backend.domain.UserLifecycleStatus;
import com.bbluxurycars.backend.domain.UserRole;
import com.bbluxurycars.backend.domain.VerificationStatus;

/**
 * Who the caller is, which tenant they act for, and what they are allowed to
 * do. The server-side counterpart of the Android app's {@code TenantContext},
 * which derives the same facts by reading Firestore on the device.
 *
 * @param uid                the verified Firebase uid; never null
 * @param provisioned        whether a mirrored {@code app_user} row was found
 * @param companyId          the owning tenant, or null when unprovisioned or unlinked
 * @param role               the caller's role
 * @param userStatus         the caller's own lifecycle status
 * @param companyStatus      the owning tenant's lifecycle status
 * @param verificationStatus the caller's KYC status
 */
public record TenantContext(
        String uid,
        boolean provisioned,
        String companyId,
        UserRole role,
        UserLifecycleStatus userStatus,
        CompanyLifecycleStatus companyStatus,
        VerificationStatus verificationStatus) {

    /**
     * The caller authenticated successfully but has no mirrored row yet.
     *
     * <p>This is the expected state until the Firestore-to-Postgres backfill
     * lands, and it is reported rather than treated as an error: the user
     * genuinely exists in Firebase Auth, so a 404 would be a lie and a 503
     * would take a working app offline. Callers see {@code provisioned=false}
     * and keep using their existing Firestore path.
     *
     * <p>Defaults are the least-privileged value of each enum, so a caller that
     * ignores the flag and reads the fields anyway is denied rather than
     * allowed.
     */
    public static TenantContext unprovisioned(String uid) {
        return new TenantContext(
                uid,
                false,
                null,
                UserRole.UNKNOWN,
                UserLifecycleStatus.PENDING_COMPANY_APPROVAL,
                CompanyLifecycleStatus.PENDING_REVIEW,
                VerificationStatus.NOT_STARTED);
    }

    public boolean hasTenantScope() {
        return companyId != null && !companyId.isBlank();
    }

    /** True when this caller is an active admin of an operational tenant. */
    public boolean isActiveAdmin() {
        return role == UserRole.ADMIN
                && userStatus == UserLifecycleStatus.ACTIVE
                && companyStatus.isOperational();
    }

    /**
     * Whether the caller may create a booking. Mirrors the conditions
     * {@code firestore.rules} enforces today for {@code rental_requests}
     * create: a verified client, in an operational tenant, whose own account is
     * active.
     */
    public boolean canBook() {
        return role == UserRole.CLIENT
                && userStatus == UserLifecycleStatus.ACTIVE
                && companyStatus.isOperational()
                && verificationStatus.allowsBooking()
                && hasTenantScope();
    }
}
