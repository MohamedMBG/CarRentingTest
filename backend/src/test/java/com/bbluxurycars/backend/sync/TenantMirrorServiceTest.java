package com.bbluxurycars.backend.sync;

import com.bbluxurycars.backend.domain.AppUser;
import com.bbluxurycars.backend.domain.Car;
import com.bbluxurycars.backend.domain.Company;
import com.bbluxurycars.backend.domain.CompanyLifecycleStatus;
import com.bbluxurycars.backend.domain.UserLifecycleStatus;
import com.bbluxurycars.backend.domain.UserRole;
import com.bbluxurycars.backend.domain.VerificationStatus;
import com.bbluxurycars.backend.firestore.FirestoreDocument;
import com.bbluxurycars.backend.repository.AppUserRepository;
import com.bbluxurycars.backend.repository.CarRepository;
import com.bbluxurycars.backend.repository.CompanyRepository;
import com.bbluxurycars.backend.support.AbstractPostgresIntegrationTest;
import com.bbluxurycars.backend.support.FirestoreTestConfig;
import com.bbluxurycars.backend.support.InMemoryFirestoreGateway;
import com.bbluxurycars.backend.sync.TenantMirrorService.MirrorSummary;
import com.bbluxurycars.backend.tenant.TenantContext;
import com.bbluxurycars.backend.tenant.TenantContextService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The Firestore-to-Postgres mirror, against a real Postgres and a stand-in
 * Firestore.
 *
 * <p>The foreign-key ordering rule (companies before users) is asserted rather
 * than assumed: it is the constraint that makes a naive backfill fail, and the
 * one a future change is most likely to break.
 */
@Transactional
@Import(FirestoreTestConfig.class)
class TenantMirrorServiceTest extends AbstractPostgresIntegrationTest {

    private static final String TENANT = "company-mirror";

    @Autowired
    private TenantMirrorService tenantMirrorService;

    @Autowired
    private TenantContextService tenantContextService;

    @Autowired
    private InMemoryFirestoreGateway firestore;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void seedFirestore() {
        firestore.clear();
        firestore.put("companies", TENANT, Map.of(
                "name", "Mirror Motors",
                "phone", "+212600000000",
                "address", "12 Rue Test, Casablanca",
                "status", "approved",
                "location", new FirestoreDocument.GeoPointValue(33.5731, -7.5898)));
        firestore.put("users", "uid-mirror-client", Map.of(
                "name", "Sara Client",
                "email", "sara@example.com",
                "phone", "+212611111111",
                "role", "client",
                "companyId", TENANT,
                "status", "active",
                "verification_status", "approved"));
        firestore.put("cars", "car-mirror-1", Map.of(
                "model", "Dacia Duster",
                "type", "SUV",
                "pricePerDay", 449.99,
                "seats", 5,
                "transmissionType", "manual",
                "imageUrl", "https://example.com/duster.jpg",
                "available", true,
                "maintenance", false,
                "companyId", TENANT));
    }

    @Test
    void mirrorsACompanyWithItsFieldsAndStatus() {
        tenantMirrorService.mirrorTenant(TENANT);
        flushAndDetachAll();

        Company company = companyRepository.findById(TENANT).orElseThrow();
        assertThat(company.getName()).isEqualTo("Mirror Motors");
        assertThat(company.getStatus()).isEqualTo(CompanyLifecycleStatus.APPROVED);
        assertThat(company.getLocationLatitude()).isEqualTo(33.5731);
        assertThat(company.getLocationLongitude()).isEqualTo(-7.5898);
    }

    @Test
    void mirrorsUsersAndCarsOfTheTenant() {
        MirrorSummary summary = tenantMirrorService.mirrorTenant(TENANT);
        flushAndDetachAll();

        assertThat(summary.companyMirrored()).isTrue();
        assertThat(summary.usersMirrored()).isEqualTo(1);
        assertThat(summary.carsMirrored()).isEqualTo(1);

        AppUser user = appUserRepository.findByFirebaseUid("uid-mirror-client").orElseThrow();
        assertThat(user.getCompanyId()).isEqualTo(TENANT);
        assertThat(user.getRole()).isEqualTo(UserRole.CLIENT);
        assertThat(user.getFullName()).isEqualTo("Sara Client");
        assertThat(user.getStatus()).isEqualTo(UserLifecycleStatus.ACTIVE);
        assertThat(user.getVerificationStatus()).isEqualTo(VerificationStatus.APPROVED);

        Car car = carRepository.findByIdAndCompanyId("car-mirror-1", TENANT).orElseThrow();
        assertThat(car.getModel()).isEqualTo("Dacia Duster");
        assertThat(car.getSeats()).isEqualTo(5);
        assertThat(car.isBookable()).isTrue();
    }

    /**
     * Firestore holds prices as doubles. The mirrored value must be the number
     * a human typed at scale 2, not its binary approximation, or every total
     * computed from it is a cent out.
     */
    @Test
    void mirrorsAPriceAsAnExactTwoDecimalAmount() {
        tenantMirrorService.mirrorTenant(TENANT);
        flushAndDetachAll();

        Car car = carRepository.findByIdAndCompanyId("car-mirror-1", TENANT).orElseThrow();
        assertThat(car.getPricePerDay()).isEqualByComparingTo("449.99");
        assertThat(car.getPricePerDay().scale()).isEqualTo(2);
    }

    /**
     * Mirroring one user must bring their company across first, or the foreign
     * key on {@code app_user.company_id} rejects the insert.
     */
    @Test
    void mirrorsTheCompanyBeforeTheUserThatReferencesIt() {
        assertThat(companyRepository.findById(TENANT)).isEmpty();

        Optional<AppUser> mirrored = tenantMirrorService.mirrorUser("uid-mirror-client");
        flushAndDetachAll();

        assertThat(mirrored).isPresent();
        assertThat(companyRepository.findById(TENANT)).isPresent();
        assertThat(appUserRepository.findByFirebaseUid("uid-mirror-client"))
                .get()
                .extracting(AppUser::getCompanyId)
                .isEqualTo(TENANT);
    }

    /**
     * A user naming a company with no document is mirrored unlinked rather than
     * failing the sync: one dangling reference must not cost the whole tenant.
     */
    @Test
    void mirrorsAUserWhoseCompanyDocumentIsMissingWithoutATenant() {
        firestore.put("users", "uid-orphan", Map.of(
                "name", "Orphan",
                "role", "client",
                "companyId", "company-that-does-not-exist",
                "verification_status", "approved"));

        Optional<AppUser> mirrored = tenantMirrorService.mirrorUser("uid-orphan");
        flushAndDetachAll();

        assertThat(mirrored).isPresent();
        AppUser stored = appUserRepository.findByFirebaseUid("uid-orphan").orElseThrow();
        assertThat(stored.hasTenantScope()).isFalse();
    }

    /** Re-running a sync must update rows, not duplicate or reject them. */
    @Test
    void isIdempotentAndPicksUpChanges() {
        tenantMirrorService.mirrorTenant(TENANT);
        flushAndDetachAll();

        Map<String, Object> updated = new HashMap<>(Map.of(
                "model", "Dacia Duster",
                "type", "SUV",
                "pricePerDay", 500.00,
                "available", false,
                "companyId", TENANT));
        firestore.put("cars", "car-mirror-1", updated);

        MirrorSummary second = tenantMirrorService.mirrorTenant(TENANT);
        flushAndDetachAll();

        assertThat(second.carsMirrored()).isEqualTo(1);
        List<Car> cars = carRepository.findAllByCompanyId(TENANT);
        assertThat(cars).hasSize(1);
        assertThat(cars.get(0).getPricePerDay()).isEqualByComparingTo("500.00");
        assertThat(cars.get(0).isBookable()).isFalse();
    }

    /**
     * The first authenticated request provisions the caller, so a user reaches
     * the API without waiting for anyone to run a backfill.
     */
    @Test
    void resolvingAnUnmirroredCallerProvisionsThemFromFirestore() {
        TenantContext context = tenantContextService.resolve("uid-mirror-client");

        assertThat(context.provisioned()).isTrue();
        assertThat(context.companyId()).isEqualTo(TENANT);
        assertThat(context.canBook()).isTrue();
    }

    /**
     * Firestore being unreachable leaves the caller unprovisioned -- the state
     * the client already handles -- rather than failing the request.
     */
    @Test
    void resolvingStaysUnprovisionedWhenFirestoreIsUnavailable() {
        firestore.setAvailable(false);

        TenantContext context = tenantContextService.resolve("uid-mirror-client");

        assertThat(context.provisioned()).isFalse();
        assertThat(context.canBook()).isFalse();
    }

    @Test
    void reportsNothingMirroredWhenTheCompanyDocumentIsAbsent() {
        MirrorSummary summary = tenantMirrorService.mirrorTenant("company-unknown");

        assertThat(summary.companyMirrored()).isFalse();
        assertThat(summary.usersMirrored()).isZero();
        assertThat(summary.carsMirrored()).isZero();
    }

    private void flushAndDetachAll() {
        entityManager.flush();
        entityManager.clear();
    }
}
