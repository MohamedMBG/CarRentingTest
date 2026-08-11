package com.bbluxurycars.backend.sync;

import com.bbluxurycars.backend.domain.AppUser;
import com.bbluxurycars.backend.domain.Car;
import com.bbluxurycars.backend.domain.Company;
import com.bbluxurycars.backend.domain.CompanyLifecycleStatus;
import com.bbluxurycars.backend.domain.UserLifecycleStatus;
import com.bbluxurycars.backend.domain.UserRole;
import com.bbluxurycars.backend.domain.VerificationStatus;
import com.bbluxurycars.backend.firestore.FirestoreDocument;
import com.bbluxurycars.backend.firestore.FirestoreGateway;
import com.bbluxurycars.backend.repository.AppUserRepository;
import com.bbluxurycars.backend.repository.CarRepository;
import com.bbluxurycars.backend.repository.CompanyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

/**
 * Copies Firestore documents into Postgres.
 *
 * <p>This is the migration mechanism from docs/SAAS_ROADMAP.md 5.3, and it
 * makes the booking API usable at all: those endpoints resolve a tenant from a
 * mirrored {@code app_user} row, and until one exists every caller is
 * unprovisioned.
 *
 * <p>Two rules the schema imposes and this class obeys:
 *
 * <ul>
 *   <li><b>Companies before users.</b> {@code app_user.company_id} carries a
 *       foreign key, so a user referencing an unmirrored company is rejected
 *       outright.</li>
 *   <li><b>Firestore is the source, never the target.</b> The gateway is
 *       read-only: the app still owns those documents, and a second writer
 *       without a shared transaction produces divergence nobody can
 *       reconstruct.</li>
 * </ul>
 *
 * <p>Every mirror is an upsert keyed by the Firestore id, so re-running it is
 * harmless -- important, because it runs both on demand per user and in bulk
 * per tenant.
 */
@Service
public class TenantMirrorService {

    private static final Logger log = LoggerFactory.getLogger(TenantMirrorService.class);

    private static final String USERS = "users";
    private static final String COMPANIES = "companies";
    private static final String CARS = "cars";

    /** Firestore field names, which are the app's own and not always camelCase. */
    private static final String FIELD_COMPANY_ID = "companyId";
    private static final String FIELD_VERIFICATION_STATUS = "verification_status";

    private final FirestoreGateway firestore;
    private final CompanyRepository companyRepository;
    private final AppUserRepository appUserRepository;
    private final CarRepository carRepository;

    public TenantMirrorService(FirestoreGateway firestore,
                               CompanyRepository companyRepository,
                               AppUserRepository appUserRepository,
                               CarRepository carRepository) {
        this.firestore = firestore;
        this.companyRepository = companyRepository;
        this.appUserRepository = appUserRepository;
        this.carRepository = carRepository;
    }

    /** What a bulk sync did, so an operator can see it worked. */
    public record MirrorSummary(boolean companyMirrored, int usersMirrored, int carsMirrored) {

        static MirrorSummary nothing() {
            return new MirrorSummary(false, 0, 0);
        }
    }

    /**
     * Mirrors one user and, first, the company they belong to.
     *
     * <p>Called when a verified caller has no mirrored row yet, so the very
     * first authenticated request provisions the caller instead of waiting for
     * a bulk backfill. Returns empty when Firestore is unavailable or the user
     * document does not exist -- both leave the caller unprovisioned, which is
     * a supported state rather than an error.
     */
    @Transactional
    public Optional<AppUser> mirrorUser(String uid) {
        if (!firestore.isAvailable() || uid == null || uid.isBlank()) {
            return Optional.empty();
        }
        Optional<FirestoreDocument> document = firestore.findDocument(USERS, uid);
        if (document.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(upsertUser(document.get()));
    }

    /**
     * Mirrors a whole tenant: the company, its users, and its fleet.
     *
     * <p>Intended for the initial backfill and for an operator forcing a
     * refresh. Ordering matters here for the same foreign-key reason as above,
     * and cars are last because a booking cannot exist without both a car and a
     * renter anyway.
     */
    @Transactional
    public MirrorSummary mirrorTenant(String companyId) {
        if (!firestore.isAvailable() || companyId == null || companyId.isBlank()) {
            return MirrorSummary.nothing();
        }

        boolean companyMirrored = mirrorCompany(companyId).isPresent();
        if (!companyMirrored) {
            // Without the company row every user and car insert would fail its
            // foreign key one by one. Stopping here reports the actual problem.
            log.warn("Tenant {} not mirrored: no companies/{} document", companyId, companyId);
            return MirrorSummary.nothing();
        }

        List<FirestoreDocument> users = firestore.findWhereEquals(USERS, FIELD_COMPANY_ID, companyId);
        users.forEach(this::upsertUser);

        List<FirestoreDocument> cars = firestore.findWhereEquals(CARS, FIELD_COMPANY_ID, companyId);
        cars.forEach(car -> upsertCar(car, companyId));

        return new MirrorSummary(true, users.size(), cars.size());
    }

    private Optional<Company> mirrorCompany(String companyId) {
        return firestore.findDocument(COMPANIES, companyId).map(document -> {
            Company company = companyRepository.findById(document.id())
                    .orElseGet(() -> new Company(
                            document.id(),
                            document.getString("name").orElse("Unnamed agency")));
            document.getString("name").ifPresent(company::setName);
            document.getString("phone").ifPresent(company::setPhone);
            document.getString("address").ifPresent(company::setAddress);
            company.setStatus(CompanyLifecycleStatus.from(document.getString("status").orElse(null)));
            document.getGeoPoint("location")
                    .ifPresent(point -> company.setLocation(point.latitude(), point.longitude()));
            return companyRepository.save(company);
        });
    }

    private AppUser upsertUser(FirestoreDocument document) {
        UserRole role = UserRole.from(document.getString("role").orElse(null));
        AppUser user = appUserRepository.findByFirebaseUid(document.id())
                .orElseGet(() -> new AppUser(document.id(), role));
        user.setRole(role);
        document.getString("email").ifPresent(user::setEmail);
        // The app writes the user's display name under `name`; the mirror calls
        // it fullName, which is what the column has always been.
        document.getString("name").ifPresent(user::setFullName);
        document.getString("phone").ifPresent(user::setPhone);
        user.setStatus(UserLifecycleStatus.from(document.getString("status").orElse(null), role));
        user.setVerificationStatus(
                VerificationStatus.from(document.getString(FIELD_VERIFICATION_STATUS).orElse(null)));

        String companyId = document.getString(FIELD_COMPANY_ID).orElse(null);
        if (companyId != null && companyRepository.findById(companyId).isEmpty()) {
            // Mirror the company the user names, in case this user is being
            // provisioned on their own rather than as part of a tenant sync.
            mirrorCompany(companyId);
        }
        if (companyId != null && companyRepository.findById(companyId).isPresent()) {
            user.setCompanyId(companyId);
        } else if (companyId != null) {
            // The user names a company that has no Firestore document. Leaving
            // company_id null keeps the row insertable and the caller
            // unprovisioned, which is safer than failing the whole sync over
            // one dangling reference.
            log.warn("User {} references company {} which has no document; left unlinked",
                    document.id(), companyId);
        }
        return appUserRepository.save(user);
    }

    private void upsertCar(FirestoreDocument document, String companyId) {
        BigDecimal pricePerDay = document.getDecimal("pricePerDay")
                .orElse(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
        String model = document.getString("model").orElse("Unnamed vehicle");

        Car car = carRepository.findByIdAndCompanyId(document.id(), companyId)
                .orElseGet(() -> new Car(document.id(), companyId, model, pricePerDay));
        car.setModel(model);
        car.setPricePerDay(pricePerDay);
        document.getString("type").ifPresent(car::setType);
        document.getInteger("seats").ifPresent(car::setSeats);
        document.getString("transmissionType").ifPresent(car::setTransmissionType);
        // Legacy documents carry a single imageUrl; newer ones an imageUrls
        // list whose first entry the app treats as primary. Only the primary
        // image is mirrored -- the gallery stays in Firestore, which the app
        // already reads live.
        document.getString("imageUrl").ifPresent(car::setImageUrl);
        // Absent flags default to the safe answer: a car is only offerable if
        // the document says so, and only out of maintenance if it says that
        // too.
        car.setAvailable(document.getBoolean("available").orElse(false));
        car.setMaintenance(document.getBoolean("maintenance").orElse(false));
        document.getInteger("rentalCount").ifPresent(car::setRentalCount);
        carRepository.save(car);
    }
}
