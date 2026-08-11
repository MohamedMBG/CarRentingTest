package com.bbluxurycars.backend.web;

import com.bbluxurycars.backend.domain.AppUser;
import com.bbluxurycars.backend.domain.Car;
import com.bbluxurycars.backend.domain.Company;
import com.bbluxurycars.backend.domain.CompanyLifecycleStatus;
import com.bbluxurycars.backend.domain.UserLifecycleStatus;
import com.bbluxurycars.backend.domain.UserRole;
import com.bbluxurycars.backend.domain.VerificationStatus;
import com.bbluxurycars.backend.repository.AppUserRepository;
import com.bbluxurycars.backend.repository.CarRepository;
import com.bbluxurycars.backend.repository.CompanyRepository;
import com.bbluxurycars.backend.security.FirebaseAuthFilter;
import com.bbluxurycars.backend.support.AbstractPostgresIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The wire contract of the booking endpoints, including the error shape the
 * Android client will branch on.
 *
 * <p>Built with {@code standaloneSetup} for the same reason as
 * {@code MeControllerTest}: verifying a genuine Firebase ID token needs real
 * credentials, and the filter's behaviour is a separate concern. The uid is
 * injected exactly as the filter sets it. {@link ApiExceptionHandler} is
 * registered explicitly because standalone setup does not scan for advice.
 */
@Transactional
class BookingControllerTest extends AbstractPostgresIntegrationTest {

    private static final String TENANT = "company-web";
    private static final String CAR = "car-web";
    private static final String RENTER = "uid-web-renter";
    private static final String UNVERIFIED = "uid-web-unverified";

    @Autowired
    private BookingController bookingController;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private EntityManager entityManager;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(bookingController)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();

        Company company = new Company(TENANT, "Agency Web");
        company.setStatus(CompanyLifecycleStatus.APPROVED);
        companyRepository.save(company);

        appUserRepository.save(renter(RENTER, VerificationStatus.APPROVED));
        appUserRepository.save(renter(UNVERIFIED, VerificationStatus.NOT_STARTED));
        carRepository.save(new Car(CAR, TENANT, "Wrangler", new BigDecimal("650.00")));

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void quotesAPeriodWithoutCreatingAnything() throws Exception {
        mockMvc.perform(post("/v1/bookings/quote")
                        .requestAttr(FirebaseAuthFilter.UID_ATTRIBUTE, RENTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"carId":"car-web",
                                 "startAt":"2026-11-02T09:00:00Z",
                                 "endAt":"2026-11-05T09:00:00Z"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("MAD"))
                .andExpect(jsonPath("$.rentalDays").value(3))
                .andExpect(jsonPath("$.totalPrice").value(1950.00));

        mockMvc.perform(get("/v1/bookings")
                        .requestAttr(FirebaseAuthFilter.UID_ATTRIBUTE, RENTER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void createsAPendingBookingPricedByTheServer() throws Exception {
        mockMvc.perform(post("/v1/bookings")
                        .requestAttr(FirebaseAuthFilter.UID_ATTRIBUTE, RENTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"carId":"car-web",
                                 "startAt":"2026-11-02T09:00:00Z",
                                 "endAt":"2026-11-04T09:00:00Z",
                                 "additionalRequests":"Airport pickup"}
                                """))
                .andExpect(status().isCreated())
                // Emitted as the lowercase string Firestore holds, so the app's
                // existing RentalRequestStatus.from(...) parses it unchanged.
                .andExpect(jsonPath("$.status").value("pending"))
                .andExpect(jsonPath("$.userId").value(RENTER))
                .andExpect(jsonPath("$.pricing.totalPrice").value(1300.00));
    }

    /**
     * The refusal carries a stable code so the client can route the renter to
     * verification instead of showing a generic failure.
     */
    @Test
    void refusesAnUnverifiedRenterWithAMachineReadableCode() throws Exception {
        mockMvc.perform(post("/v1/bookings")
                        .requestAttr(FirebaseAuthFilter.UID_ATTRIBUTE, UNVERIFIED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"carId":"car-web",
                                 "startAt":"2026-11-02T09:00:00Z",
                                 "endAt":"2026-11-04T09:00:00Z"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("booking_not_permitted"));
    }

    @Test
    void rejectsARequestBodyMissingTheCar() throws Exception {
        mockMvc.perform(post("/v1/bookings")
                        .requestAttr(FirebaseAuthFilter.UID_ATTRIBUTE, RENTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"startAt":"2026-11-02T09:00:00Z",
                                 "endAt":"2026-11-04T09:00:00Z"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_request"));
    }

    private static AppUser renter(String uid, VerificationStatus verification) {
        AppUser user = new AppUser(uid, UserRole.CLIENT);
        user.setCompanyId(TENANT);
        user.setStatus(UserLifecycleStatus.ACTIVE);
        user.setVerificationStatus(verification);
        return user;
    }
}
