package com.bbluxurycars.backend.repository;

import com.bbluxurycars.backend.domain.AppUser;
import com.bbluxurycars.backend.domain.Company;
import com.bbluxurycars.backend.domain.CompanyLifecycleStatus;
import com.bbluxurycars.backend.domain.UserLifecycleStatus;
import com.bbluxurycars.backend.domain.UserRole;
import com.bbluxurycars.backend.domain.VerificationStatus;
import com.bbluxurycars.backend.support.AbstractPostgresIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tenant isolation is the property this whole layer exists to guarantee, so it
 * is asserted directly against a real Postgres rather than inferred from the
 * repository method names.
 *
 * <p>{@code @Transactional} rolls each test back. Without it the rows one test
 * seeds would still be present in the next, and the count assertions would
 * depend on method execution order.
 */
@Transactional
class AppUserRepositoryTest extends AbstractPostgresIntegrationTest {

    private static final String TENANT_A = "company-a";
    private static final String TENANT_B = "company-b";

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void seedTwoTenants() {
        companyRepository.save(approvedCompany(TENANT_A, "Agency A"));
        companyRepository.save(approvedCompany(TENANT_B, "Agency B"));

        appUserRepository.save(client("user-a1", TENANT_A));
        appUserRepository.save(client("user-a2", TENANT_A));
        appUserRepository.save(client("user-b1", TENANT_B));
        flushAndDetachAll();
    }

    @Test
    void findAllByCompanyIdReturnsOnlyThatTenantsUsers() {
        List<AppUser> tenantAUsers = appUserRepository.findAllByCompanyId(TENANT_A);

        assertThat(tenantAUsers)
                .extracting(AppUser::getId)
                .containsExactlyInAnyOrder("user-a1", "user-a2");
    }

    @Test
    void findByIdAndCompanyIdReturnsTheRowWhenTheTenantMatches() {
        Optional<AppUser> found = appUserRepository.findByIdAndCompanyId("user-a1", TENANT_A);

        assertThat(found).isPresent();
        assertThat(found.get().getCompanyId()).isEqualTo(TENANT_A);
    }

    /**
     * The central guarantee: naming a real user id from another tenant is
     * indistinguishable from naming one that does not exist. Anything weaker
     * leaks the existence of other agencies' users.
     */
    @Test
    void findByIdAndCompanyIdHidesRowsBelongingToAnotherTenant() {
        Optional<AppUser> crossTenant = appUserRepository.findByIdAndCompanyId("user-b1", TENANT_A);
        Optional<AppUser> nonExistent = appUserRepository.findByIdAndCompanyId("no-such-user", TENANT_A);

        assertThat(crossTenant).isEmpty();
        assertThat(nonExistent).isEmpty();
    }

    @Test
    void countByCompanyIdCountsOnlyThatTenant() {
        assertThat(appUserRepository.countByCompanyId(TENANT_A)).isEqualTo(2);
        assertThat(appUserRepository.countByCompanyId(TENANT_B)).isEqualTo(1);
    }

    /**
     * Statuses must survive a round trip as the lowercase strings the CHECK
     * constraints permit. A converter regression would either fail the
     * constraint on write or read back a different constant.
     */
    @Test
    void enumStatusesRoundTripThroughTheirStoredStringValues() {
        AppUser admin = new AppUser("user-admin", UserRole.ADMIN);
        admin.setCompanyId(TENANT_A);
        admin.setStatus(UserLifecycleStatus.SUSPENDED);
        admin.setVerificationStatus(VerificationStatus.UNDER_REVIEW);
        appUserRepository.save(admin);
        flushAndDetachAll();

        AppUser reloaded = appUserRepository.findByFirebaseUid("user-admin").orElseThrow();

        assertThat(reloaded.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(reloaded.getStatus()).isEqualTo(UserLifecycleStatus.SUSPENDED);
        assertThat(reloaded.getVerificationStatus()).isEqualTo(VerificationStatus.UNDER_REVIEW);
    }

    /**
     * An admin stored as genuinely active must read back active. The
     * role-dependent fallback in {@code UserLifecycleStatus.from} applies only
     * to unrecognised values, and must not reclassify a valid one.
     */
    @Test
    void activeAdminRoundTripsAsActive() {
        AppUser admin = new AppUser("user-active-admin", UserRole.ADMIN);
        admin.setCompanyId(TENANT_A);
        admin.setStatus(UserLifecycleStatus.ACTIVE);
        appUserRepository.save(admin);
        flushAndDetachAll();

        AppUser reloaded = appUserRepository.findByFirebaseUid("user-active-admin").orElseThrow();

        assertThat(reloaded.getStatus()).isEqualTo(UserLifecycleStatus.ACTIVE);
    }

    @Test
    void newAdminDefaultsToPendingCompanyApproval() {
        AppUser admin = new AppUser("user-new-admin", UserRole.ADMIN);

        assertThat(admin.getStatus()).isEqualTo(UserLifecycleStatus.PENDING_COMPANY_APPROVAL);
    }

    @Test
    void newClientDefaultsToActive() {
        AppUser newClient = new AppUser("user-new-client", UserRole.CLIENT);

        assertThat(newClient.getStatus()).isEqualTo(UserLifecycleStatus.ACTIVE);
    }

    /**
     * Forces pending writes to the database and empties the persistence
     * context. Without the clear, a subsequent read is served from the
     * first-level cache and returns the very instance just saved, so the
     * columns and their converters are never exercised.
     */
    private void flushAndDetachAll() {
        entityManager.flush();
        entityManager.clear();
    }

    private static Company approvedCompany(String id, String name) {
        Company company = new Company(id, name);
        company.setStatus(CompanyLifecycleStatus.APPROVED);
        return company;
    }

    private static AppUser client(String id, String companyId) {
        AppUser user = new AppUser(id, UserRole.CLIENT);
        user.setCompanyId(companyId);
        user.setVerificationStatus(VerificationStatus.APPROVED);
        return user;
    }
}
