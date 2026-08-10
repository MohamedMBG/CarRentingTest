package com.bbluxurycars.backend.tenant;

import com.bbluxurycars.backend.domain.AppUser;
import com.bbluxurycars.backend.domain.Company;
import com.bbluxurycars.backend.domain.CompanyLifecycleStatus;
import com.bbluxurycars.backend.domain.UserLifecycleStatus;
import com.bbluxurycars.backend.domain.UserRole;
import com.bbluxurycars.backend.domain.VerificationStatus;
import com.bbluxurycars.backend.repository.AppUserRepository;
import com.bbluxurycars.backend.repository.CompanyRepository;
import com.bbluxurycars.backend.support.AbstractPostgresIntegrationTest;
import jakarta.persistence.EntityManager;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Covers the single place the backend decides which tenant a caller acts for.
 * The permission assertions matter most: they are what other code will trust
 * instead of re-deriving authorization per endpoint.
 */
@Transactional
class TenantContextServiceTest extends AbstractPostgresIntegrationTest {

    private static final String TENANT = "company-1";

    @Autowired
    private TenantContextService tenantContextService;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private EntityManager entityManager;

    /**
     * The expected state for most users until the Firestore backfill lands. It
     * must report the caller's real uid while denying every permission, so an
     * app that ignores the flag is locked out rather than let in.
     */
    @Test
    void unknownUidResolvesToAnUnprovisionedContextWithNoPermissions() {
        TenantContext context = tenantContextService.resolve("uid-with-no-mirror-row");

        assertThat(context.uid()).isEqualTo("uid-with-no-mirror-row");
        assertThat(context.provisioned()).isFalse();
        assertThat(context.companyId()).isNull();
        assertThat(context.role()).isEqualTo(UserRole.UNKNOWN);
        assertThat(context.hasTenantScope()).isFalse();
        assertThat(context.canBook()).isFalse();
        assertThat(context.isActiveAdmin()).isFalse();
    }

    @Test
    void verifiedClientInAnApprovedTenantMayBook() {
        givenCompany(TENANT, CompanyLifecycleStatus.APPROVED);
        givenUser("uid-client", UserRole.CLIENT, TENANT,
                UserLifecycleStatus.ACTIVE, VerificationStatus.APPROVED);

        TenantContext context = tenantContextService.resolve("uid-client");

        assertThat(context.provisioned()).isTrue();
        assertThat(context.companyId()).isEqualTo(TENANT);
        assertThat(context.canBook()).isTrue();
        assertThat(context.isActiveAdmin()).isFalse();
    }

    @Test
    void unverifiedClientMayNotBook() {
        givenCompany(TENANT, CompanyLifecycleStatus.APPROVED);
        givenUser("uid-unverified", UserRole.CLIENT, TENANT,
                UserLifecycleStatus.ACTIVE, VerificationStatus.UNDER_REVIEW);

        assertThat(tenantContextService.resolve("uid-unverified").canBook()).isFalse();
    }

    /**
     * The suspension path Phase 2 billing will drive: when a tenant stops
     * paying, its verified clients must stop transacting even though nothing
     * about the user changed.
     */
    @Test
    void verifiedClientOfASuspendedTenantMayNotBook() {
        givenCompany(TENANT, CompanyLifecycleStatus.SUSPENDED);
        givenUser("uid-suspended-tenant", UserRole.CLIENT, TENANT,
                UserLifecycleStatus.ACTIVE, VerificationStatus.APPROVED);

        assertThat(tenantContextService.resolve("uid-suspended-tenant").canBook()).isFalse();
    }

    @Test
    void suspendedUserOfAnApprovedTenantMayNotBook() {
        givenCompany(TENANT, CompanyLifecycleStatus.APPROVED);
        givenUser("uid-suspended-user", UserRole.CLIENT, TENANT,
                UserLifecycleStatus.SUSPENDED, VerificationStatus.APPROVED);

        assertThat(tenantContextService.resolve("uid-suspended-user").canBook()).isFalse();
    }

    @Test
    void activeAdminOfAnApprovedTenantIsRecognised() {
        givenCompany(TENANT, CompanyLifecycleStatus.APPROVED);
        givenUser("uid-admin", UserRole.ADMIN, TENANT,
                UserLifecycleStatus.ACTIVE, VerificationStatus.NOT_STARTED);

        TenantContext context = tenantContextService.resolve("uid-admin");

        assertThat(context.isActiveAdmin()).isTrue();
        // An admin is not a renter: booking is gated on KYC regardless of role.
        assertThat(context.canBook()).isFalse();
    }

    @Test
    void adminAwaitingCompanyApprovalIsNotAnActiveAdmin() {
        givenCompany(TENANT, CompanyLifecycleStatus.PENDING_REVIEW);
        givenUser("uid-pending-admin", UserRole.ADMIN, TENANT,
                UserLifecycleStatus.PENDING_COMPANY_APPROVAL, VerificationStatus.NOT_STARTED);

        assertThat(tenantContextService.resolve("uid-pending-admin").isActiveAdmin()).isFalse();
    }

    /**
     * A user cannot reference a company that is not mirrored: the foreign key
     * on {@code app_user.company_id} rejects it outright.
     *
     * <p>This is the reason {@code TenantContextService} can never actually
     * observe a dangling tenant reference, and its PENDING_REVIEW fallback for
     * a missing company is defence in depth rather than a live code path. It
     * also fixes an ordering requirement on the Firestore mirror: a company
     * must be synced before any of its users, which the sync must respect.
     */
    @Test
    void userCannotReferenceACompanyThatIsNotMirrored() {
        assertThatThrownBy(() -> givenUser("uid-dangling", UserRole.CLIENT, "company-not-mirrored",
                UserLifecycleStatus.ACTIVE, VerificationStatus.APPROVED))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void userWithNoCompanyHasNoTenantScope() {
        givenUser("uid-no-company", UserRole.CLIENT, null,
                UserLifecycleStatus.ACTIVE, VerificationStatus.APPROVED);

        TenantContext context = tenantContextService.resolve("uid-no-company");

        assertThat(context.provisioned()).isTrue();
        assertThat(context.hasTenantScope()).isFalse();
        assertThat(context.canBook()).isFalse();
    }

    @Test
    void blankUidIsRejected() {
        assertThatThrownBy(() -> tenantContextService.resolve("  "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tenantContextService.resolve(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private void givenCompany(String id, CompanyLifecycleStatus status) {
        Company company = new Company(id, "Agency " + id);
        company.setStatus(status);
        companyRepository.save(company);
        entityManager.flush();
        entityManager.clear();
    }

    private void givenUser(String uid,
                           UserRole role,
                           String companyId,
                           UserLifecycleStatus status,
                           VerificationStatus verificationStatus) {
        AppUser user = new AppUser(uid, role);
        user.setCompanyId(companyId);
        user.setStatus(status);
        user.setVerificationStatus(verificationStatus);
        appUserRepository.save(user);
        entityManager.flush();
        entityManager.clear();
    }
}
