package com.bbluxurycars.backend.booking;

import com.bbluxurycars.backend.domain.AppUser;
import com.bbluxurycars.backend.domain.Car;
import com.bbluxurycars.backend.domain.Company;
import com.bbluxurycars.backend.domain.CompanyLifecycleStatus;
import com.bbluxurycars.backend.domain.RentalRequest;
import com.bbluxurycars.backend.domain.RentalRequestStatus;
import com.bbluxurycars.backend.domain.UserLifecycleStatus;
import com.bbluxurycars.backend.domain.UserRole;
import com.bbluxurycars.backend.domain.VerificationStatus;
import com.bbluxurycars.backend.repository.AppUserRepository;
import com.bbluxurycars.backend.repository.CarRepository;
import com.bbluxurycars.backend.repository.CompanyRepository;
import com.bbluxurycars.backend.support.AbstractPostgresIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The authorization and pricing rules that used to live on the device.
 *
 * <p>Each test states one thing a tampered or merely out-of-date client must
 * not be able to achieve.
 */
@Transactional
class BookingServiceTest extends AbstractPostgresIntegrationTest {

    private static final String TENANT = "company-svc";
    private static final String OTHER_TENANT = "company-svc-other";
    private static final String CAR = "car-svc";
    private static final String OTHER_TENANT_CAR = "car-svc-other";
    private static final String RENTER = "uid-svc-renter";
    private static final String ADMIN = "uid-svc-admin";
    private static final String UNVERIFIED = "uid-svc-unverified";
    private static final Instant MONDAY = Instant.parse("2026-10-05T09:00:00Z");

    @Autowired
    private BookingService bookingService;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void seed() {
        companyRepository.save(approvedCompany(TENANT));
        companyRepository.save(approvedCompany(OTHER_TENANT));

        appUserRepository.save(renter(RENTER, TENANT, VerificationStatus.APPROVED));
        appUserRepository.save(renter(UNVERIFIED, TENANT, VerificationStatus.SUBMITTED));

        AppUser admin = new AppUser(ADMIN, UserRole.ADMIN);
        admin.setCompanyId(TENANT);
        admin.setStatus(UserLifecycleStatus.ACTIVE);
        appUserRepository.save(admin);

        carRepository.save(new Car(CAR, TENANT, "Defender", new BigDecimal("700.00")));
        carRepository.save(new Car(OTHER_TENANT_CAR, OTHER_TENANT, "Cayenne", new BigDecimal("1200.00")));

        entityManager.flush();
        entityManager.clear();
    }

    /**
     * The core of the slice: the stored total comes from the car row. There is
     * no request field through which a client could propose another figure, and
     * this asserts the number that lands in the database.
     */
    @Test
    void pricesABookingFromTheCarRow() {
        RentalRequest created = bookingService.create(
                RENTER, CAR, MONDAY, MONDAY.plus(Duration.ofDays(3)), "Child seat please");

        assertThat(created.getPricing().getUnitPricePerDay()).isEqualByComparingTo("700.00");
        assertThat(created.getPricing().getRentalDays()).isEqualTo(3);
        assertThat(created.getPricing().getTotalPrice()).isEqualByComparingTo("2100.00");
        assertThat(created.getStatus()).isEqualTo(RentalRequestStatus.PENDING);
        assertThat(created.getCompanyId()).isEqualTo(TENANT);
        assertThat(created.getUserId()).isEqualTo(RENTER);
    }

    @Test
    void refusesToBookForAnUnverifiedRenter() {
        assertThatThrownBy(() -> bookingService.create(
                UNVERIFIED, CAR, MONDAY, MONDAY.plus(Duration.ofDays(1)), null))
                .isInstanceOfSatisfying(BookingException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(e.getCode()).isEqualTo("booking_not_permitted");
                });
    }

    /**
     * Another tenant's car reads as absent rather than forbidden, so the
     * endpoint cannot be used to discover that the vehicle exists.
     */
    @Test
    void treatsAnotherTenantsCarAsNonExistent() {
        assertThatThrownBy(() -> bookingService.create(
                RENTER, OTHER_TENANT_CAR, MONDAY, MONDAY.plus(Duration.ofDays(1)), null))
                .isInstanceOfSatisfying(BookingException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void refusesToBookAVehicleInMaintenance() {
        Car car = carRepository.findByIdAndCompanyId(CAR, TENANT).orElseThrow();
        car.setMaintenance(true);
        entityManager.flush();

        assertThatThrownBy(() -> bookingService.create(
                RENTER, CAR, MONDAY, MONDAY.plus(Duration.ofDays(1)), null))
                .isInstanceOfSatisfying(BookingException.class,
                        e -> assertThat(e.getCode()).isEqualTo("car_not_bookable"));
    }

    @Test
    void refusesAPeriodThatDoesNotRunForwards() {
        assertThatThrownBy(() -> bookingService.create(
                RENTER, CAR, MONDAY, MONDAY.minus(Duration.ofHours(2)), null))
                .isInstanceOf(com.bbluxurycars.backend.pricing.InvalidRentalPeriodException.class);
    }

    @Test
    void anAdminApprovesAPendingBooking() {
        RentalRequest created = bookingService.create(
                RENTER, CAR, MONDAY, MONDAY.plus(Duration.ofDays(2)), null);

        RentalRequest approved = bookingService.approve(ADMIN, created.getId());

        assertThat(approved.getStatus()).isEqualTo(RentalRequestStatus.APPROVED);
    }

    /** The double-booking the client could not prevent. */
    @Test
    void refusesToApproveASecondBookingOverlappingAnApprovedOne() {
        RentalRequest first = bookingService.create(
                RENTER, CAR, MONDAY, MONDAY.plus(Duration.ofDays(3)), null);
        RentalRequest second = bookingService.create(
                RENTER, CAR, MONDAY.plus(Duration.ofDays(2)), MONDAY.plus(Duration.ofDays(5)), null);

        bookingService.approve(ADMIN, first.getId());

        assertThatThrownBy(() -> bookingService.approve(ADMIN, second.getId()))
                .isInstanceOfSatisfying(BookingException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getCode()).isEqualTo("dates_already_held");
                });
    }

    @Test
    void aRenterMayNotApproveTheirOwnBooking() {
        RentalRequest created = bookingService.create(
                RENTER, CAR, MONDAY, MONDAY.plus(Duration.ofDays(1)), null);

        assertThatThrownBy(() -> bookingService.approve(RENTER, created.getId()))
                .isInstanceOfSatisfying(BookingException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    /** Terminal states are terminal: a rejection cannot be undone by approving. */
    @Test
    void refusesAnIllegalStatusTransition() {
        RentalRequest created = bookingService.create(
                RENTER, CAR, MONDAY, MONDAY.plus(Duration.ofDays(1)), null);
        bookingService.reject(ADMIN, created.getId());

        assertThatThrownBy(() -> bookingService.approve(ADMIN, created.getId()))
                .isInstanceOfSatisfying(BookingException.class,
                        e -> assertThat(e.getCode()).isEqualTo("illegal_status_transition"));
    }

    @Test
    void completingAnApprovedBookingStampsCompletedAt() {
        RentalRequest created = bookingService.create(
                RENTER, CAR, MONDAY, MONDAY.plus(Duration.ofDays(1)), null);
        bookingService.approve(ADMIN, created.getId());

        RentalRequest completed = bookingService.complete(ADMIN, created.getId());

        assertThat(completed.getStatus()).isEqualTo(RentalRequestStatus.COMPLETED);
        assertThat(completed.getCompletedAt()).isNotNull();
    }

    /**
     * A renter sees only their own bookings; the admin sees the tenant's queue.
     * Getting this backwards would leak other renters' details to every user.
     */
    @Test
    void listsTheTenantQueueForAdminsAndOnlyOwnBookingsForRenters() {
        bookingService.create(RENTER, CAR, MONDAY, MONDAY.plus(Duration.ofDays(1)), null);
        bookingService.create(secondRenter(), CAR, MONDAY.plus(Duration.ofDays(4)),
                MONDAY.plus(Duration.ofDays(5)), null);
        entityManager.flush();

        List<RentalRequest> adminView = bookingService.listForCaller(ADMIN);
        List<RentalRequest> renterView = bookingService.listForCaller(RENTER);

        assertThat(adminView).hasSize(2);
        assertThat(renterView).extracting(RentalRequest::getUserId).containsOnly(RENTER);
    }

    /**
     * A second verified renter in the same tenant, created lazily so the list
     * test has two distinct authors without complicating the shared fixture.
     */
    private String secondRenter() {
        String uid = "uid-svc-renter-2";
        if (appUserRepository.findByFirebaseUid(uid).isEmpty()) {
            appUserRepository.save(renter(uid, TENANT, VerificationStatus.APPROVED));
            entityManager.flush();
        }
        return uid;
    }

    private static Company approvedCompany(String id) {
        Company company = new Company(id, "Agency " + id);
        company.setStatus(CompanyLifecycleStatus.APPROVED);
        return company;
    }

    private static AppUser renter(String uid, String tenantId, VerificationStatus verification) {
        AppUser user = new AppUser(uid, UserRole.CLIENT);
        user.setCompanyId(tenantId);
        user.setStatus(UserLifecycleStatus.ACTIVE);
        user.setVerificationStatus(verification);
        return user;
    }
}
