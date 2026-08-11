package com.bbluxurycars.backend.repository;

import com.bbluxurycars.backend.domain.AppUser;
import com.bbluxurycars.backend.domain.Car;
import com.bbluxurycars.backend.domain.Company;
import com.bbluxurycars.backend.domain.CompanyLifecycleStatus;
import com.bbluxurycars.backend.domain.PricingBreakdown;
import com.bbluxurycars.backend.domain.RentalRequest;
import com.bbluxurycars.backend.domain.RentalRequestStatus;
import com.bbluxurycars.backend.domain.UserRole;
import com.bbluxurycars.backend.domain.VerificationStatus;
import com.bbluxurycars.backend.support.AbstractPostgresIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Asserts the two properties the schema itself is responsible for: tenant
 * isolation, and that a vehicle cannot be held twice over overlapping dates.
 *
 * <p>Run against a real Postgres because both are enforced by Postgres --
 * a composite foreign key and a GiST exclusion constraint. Neither exists in a
 * substitute database, so a test that passed there would prove nothing.
 */
@Transactional
class RentalRequestRepositoryTest extends AbstractPostgresIntegrationTest {

    private static final String TENANT_A = "company-booking-a";
    private static final String TENANT_B = "company-booking-b";
    private static final Instant MONDAY = Instant.parse("2026-09-07T09:00:00Z");

    @Autowired
    private RentalRequestRepository rentalRequestRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void seedTwoTenants() {
        seedTenant(TENANT_A);
        seedTenant(TENANT_B);
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void findAllByCompanyIdReturnsOnlyThatTenantsBookings() {
        rentalRequestRepository.save(booking("req-a", TENANT_A, MONDAY, MONDAY.plus(Duration.ofDays(2))));
        rentalRequestRepository.save(booking("req-b", TENANT_B, MONDAY, MONDAY.plus(Duration.ofDays(2))));
        entityManager.flush();

        List<RentalRequest> tenantA =
                rentalRequestRepository.findAllByCompanyIdOrderByCreatedAtDesc(TENANT_A);

        assertThat(tenantA).extracting(RentalRequest::getId).containsExactly("req-a");
    }

    /**
     * Naming another tenant's booking id must be indistinguishable from naming
     * one that does not exist, or the endpoint becomes an existence oracle.
     */
    @Test
    void findByIdAndCompanyIdHidesBookingsOfAnotherTenant() {
        rentalRequestRepository.save(booking("req-b", TENANT_B, MONDAY, MONDAY.plus(Duration.ofDays(2))));
        entityManager.flush();

        assertThat(rentalRequestRepository.findByIdAndCompanyId("req-b", TENANT_A)).isEmpty();
        assertThat(rentalRequestRepository.findByIdAndCompanyId("req-b", TENANT_B)).isPresent();
    }

    /** The guarantee gap 3.3 of the roadmap is about: no double-booking. */
    @Test
    void refusesASecondApprovedBookingOverlappingTheFirst() {
        rentalRequestRepository.save(approved(
                booking("req-held", TENANT_A, MONDAY, MONDAY.plus(Duration.ofDays(3)))));
        entityManager.flush();

        rentalRequestRepository.save(approved(booking("req-clash", TENANT_A,
                MONDAY.plus(Duration.ofDays(2)), MONDAY.plus(Duration.ofDays(5)))));

        assertThatThrownBy(entityManager::flush)
                .hasMessageContaining("rental_request_no_overlapping_hold");
    }

    /**
     * The ranges are half-open, so handing a car back at the exact moment the
     * next rental starts is legal. A fleet that could not do this would lose a
     * day between every booking.
     */
    @Test
    void allowsBackToBackApprovedBookings() {
        rentalRequestRepository.save(approved(
                booking("req-first", TENANT_A, MONDAY, MONDAY.plus(Duration.ofDays(2)))));
        rentalRequestRepository.save(approved(booking("req-second", TENANT_A,
                MONDAY.plus(Duration.ofDays(2)), MONDAY.plus(Duration.ofDays(4)))));

        entityManager.flush();

        assertThat(rentalRequestRepository.findAllByCompanyIdOrderByCreatedAtDesc(TENANT_A))
                .hasSize(2);
    }

    /**
     * Pending requests deliberately do not hold the vehicle: competing requests
     * for the same dates are how the agency's approval queue works.
     */
    @Test
    void allowsSeveralPendingRequestsForTheSameCarAndDates() {
        rentalRequestRepository.save(booking("req-p1", TENANT_A, MONDAY, MONDAY.plus(Duration.ofDays(2))));
        rentalRequestRepository.save(booking("req-p2", TENANT_A, MONDAY, MONDAY.plus(Duration.ofDays(2))));

        entityManager.flush();

        assertThat(rentalRequestRepository.findAllByCompanyIdOrderByCreatedAtDesc(TENANT_A))
                .hasSize(2);
    }

    /**
     * A rejected request releases the dates -- otherwise a declined applicant
     * would keep a car off the market indefinitely.
     */
    @Test
    void aRejectedBookingDoesNotHoldTheDates() {
        RentalRequest rejected = booking("req-rejected", TENANT_A, MONDAY, MONDAY.plus(Duration.ofDays(3)));
        rejected.transitionTo(RentalRequestStatus.REJECTED);
        rentalRequestRepository.save(rejected);
        rentalRequestRepository.save(approved(
                booking("req-ok", TENANT_A, MONDAY, MONDAY.plus(Duration.ofDays(3)))));

        entityManager.flush();

        assertThat(rentalRequestRepository.findAllByCompanyIdOrderByCreatedAtDesc(TENANT_A))
                .hasSize(2);
    }

    /**
     * The composite foreign key, not application code, is what stops a booking
     * pointing at another tenant's vehicle.
     */
    @Test
    void refusesABookingWhoseCarBelongsToAnotherTenant() {
        RentalRequest crossTenant = new RentalRequest(
                "req-cross", TENANT_A, carIdOf(TENANT_B), userIdOf(TENANT_A),
                MONDAY, MONDAY.plus(Duration.ofDays(1)), pricing());
        rentalRequestRepository.save(crossTenant);

        assertThatThrownBy(entityManager::flush)
                .hasMessageContaining("rental_request_car_same_tenant");
    }

    private void seedTenant(String tenantId) {
        Company company = new Company(tenantId, "Agency " + tenantId);
        company.setStatus(CompanyLifecycleStatus.APPROVED);
        companyRepository.save(company);

        AppUser renter = new AppUser(userIdOf(tenantId), UserRole.CLIENT);
        renter.setCompanyId(tenantId);
        renter.setVerificationStatus(VerificationStatus.APPROVED);
        appUserRepository.save(renter);

        carRepository.save(new Car(carIdOf(tenantId), tenantId, "Range Rover", new BigDecimal("900.00")));
    }

    private RentalRequest booking(String id, String tenantId, Instant start, Instant end) {
        return new RentalRequest(id, tenantId, carIdOf(tenantId), userIdOf(tenantId),
                start, end, pricing());
    }

    private static RentalRequest approved(RentalRequest request) {
        request.transitionTo(RentalRequestStatus.APPROVED);
        return request;
    }

    private static PricingBreakdown pricing() {
        return new PricingBreakdown("MAD", new BigDecimal("900.00"), 2,
                new BigDecimal("1800.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("1800.00"));
    }

    private static String carIdOf(String tenantId) {
        return "car-" + tenantId;
    }

    private static String userIdOf(String tenantId) {
        return "user-" + tenantId;
    }
}
