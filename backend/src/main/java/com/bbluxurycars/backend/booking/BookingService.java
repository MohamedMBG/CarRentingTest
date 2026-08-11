package com.bbluxurycars.backend.booking;

import com.bbluxurycars.backend.domain.Car;
import com.bbluxurycars.backend.domain.PricingBreakdown;
import com.bbluxurycars.backend.domain.RentalRequest;
import com.bbluxurycars.backend.domain.RentalRequestStatus;
import com.bbluxurycars.backend.pricing.PricingService;
import com.bbluxurycars.backend.repository.CarRepository;
import com.bbluxurycars.backend.repository.RentalRequestRepository;
import com.bbluxurycars.backend.tenant.TenantContext;
import com.bbluxurycars.backend.tenant.TenantContextService;
import jakarta.persistence.EntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Server-authoritative booking: the one place a rental request is priced,
 * authorised and moved through its lifecycle.
 *
 * <p>Every method starts from a uid that {@code FirebaseAuthFilter} verified,
 * resolves the tenant from it, and scopes every read to that tenant. No method
 * accepts a company id from the caller -- doing so would let a caller simply
 * name someone else's tenant (docs/SAAS_ROADMAP.md 5.1).
 *
 * <p>Two guarantees this class exists to provide, both previously absent:
 * a booking's price comes from the car row and never from the request body, and
 * two approved bookings cannot hold the same vehicle over overlapping dates.
 */
@Service
public class BookingService {

    /** Name of the exclusion constraint declared in V2. */
    private static final String OVERLAP_CONSTRAINT = "rental_request_no_overlapping_hold";

    private static final List<RentalRequestStatus> HOLDING_STATUSES =
            List.of(RentalRequestStatus.APPROVED, RentalRequestStatus.COMPLETED);

    private final TenantContextService tenantContextService;
    private final CarRepository carRepository;
    private final RentalRequestRepository rentalRequestRepository;
    private final PricingService pricingService;
    private final EntityManager entityManager;

    public BookingService(TenantContextService tenantContextService,
                          CarRepository carRepository,
                          RentalRequestRepository rentalRequestRepository,
                          PricingService pricingService,
                          EntityManager entityManager) {
        this.tenantContextService = tenantContextService;
        this.carRepository = carRepository;
        this.rentalRequestRepository = rentalRequestRepository;
        this.pricingService = pricingService;
        this.entityManager = entityManager;
    }

    /**
     * Prices a period for a car without creating anything.
     *
     * <p>Open to any member of the tenant, not only callers who may book:
     * a renter part-way through verification still needs to see prices, and
     * quoting grants nothing.
     */
    @Transactional(readOnly = true)
    public PricingBreakdown quote(String uid, String carId, Instant startAt, Instant endAt) {
        TenantContext context = tenantContextService.resolve(uid);
        Car car = requireCarInTenant(context, carId);
        return pricingService.quote(car, startAt, endAt);
    }

    /**
     * Creates a pending booking priced from the car row.
     *
     * <p>Overlap is not checked here on purpose: a pending request does not
     * hold the vehicle, so several renters may request the same car for the
     * same dates and the agency admin chooses between them -- the behaviour the
     * app already has. The conflict is resolved at approval.
     */
    @Transactional
    public RentalRequest create(String uid, String carId, Instant startAt, Instant endAt,
                                String additionalRequests) {
        TenantContext context = tenantContextService.resolve(uid);
        if (!context.canBook()) {
            throw BookingException.notAllowedToBook(bookingRefusalReason(context));
        }

        Car car = requireCarInTenant(context, carId);
        if (!car.isBookable()) {
            throw BookingException.carNotBookable();
        }

        // Priced from the car row, never from anything the client sent. This is
        // the line that makes a tampered client unable to set its own price.
        PricingBreakdown pricing = pricingService.quote(car, startAt, endAt);

        RentalRequest request = new RentalRequest(
                UUID.randomUUID().toString(),
                context.companyId(),
                car.getId(),
                context.uid(),
                startAt,
                endAt,
                pricing);
        request.setAdditionalRequests(additionalRequests);
        return rentalRequestRepository.save(request);
    }

    /**
     * What the caller is entitled to see: an agency admin gets the tenant's
     * whole queue, a renter gets only their own bookings.
     */
    @Transactional(readOnly = true)
    public List<RentalRequest> listForCaller(String uid) {
        TenantContext context = tenantContextService.resolve(uid);
        if (!context.hasTenantScope()) {
            // An unprovisioned or unlinked caller has no bookings rather than
            // an error: this is the normal state during the Firestore backfill.
            return List.of();
        }
        if (context.isActiveAdmin()) {
            return rentalRequestRepository.findAllByCompanyIdOrderByCreatedAtDesc(context.companyId());
        }
        return rentalRequestRepository
                .findAllByCompanyIdAndUserIdOrderByCreatedAtDesc(context.companyId(), context.uid());
    }

    /**
     * Approves a booking, taking the vehicle off the market for its dates.
     *
     * <p>This is where the overlap guarantee is exercised. The advisory query
     * below only produces a good error message; the authority is the exclusion
     * constraint, and the explicit flush is what makes it speak before the
     * transaction ends -- without it the violation would surface at commit,
     * outside this method's reach, as a 500.
     */
    @Transactional
    public RentalRequest approve(String uid, String bookingId) {
        TenantContext context = requireActiveAdmin(uid);
        RentalRequest request = requireBookingInTenant(context, bookingId);

        // Queried before the status is changed, not after: with FlushMode.AUTO
        // a pending write is flushed to make the query see it, so approving
        // first would let the constraint fire from inside the query instead of
        // from the flush below, where it is handled.
        List<RentalRequest> conflicts = rentalRequestRepository
                .findAllByCompanyIdAndCarIdAndStatusInAndStartAtLessThanAndEndAtGreaterThan(
                        context.companyId(),
                        request.getCarId(),
                        HOLDING_STATUSES,
                        request.getEndAt(),
                        request.getStartAt());
        boolean conflictsWithAnother = conflicts.stream()
                .anyMatch(other -> !other.getId().equals(request.getId()));
        if (conflictsWithAnother) {
            throw BookingException.datesAlreadyHeld();
        }

        applyTransition(request, RentalRequestStatus.APPROVED);
        try {
            entityManager.flush();
        } catch (RuntimeException e) {
            // Reached when a concurrent approval committed between the query
            // above and this flush -- the race the constraint exists for.
            if (isOverlapViolation(e)) {
                throw BookingException.datesAlreadyHeld();
            }
            throw e;
        }
        return request;
    }

    /**
     * Recognises the exclusion constraint by name anywhere in the cause chain.
     *
     * <p>Matching the name rather than the exception type because the type
     * varies: a flush on an injected {@code EntityManager} raises Hibernate's
     * own {@code ConstraintViolationException} wrapped in a
     * {@code PersistenceException}, while the same failure through a repository
     * arrives already translated to {@link DataIntegrityViolationException}.
     * Treating every integrity failure as an overlap would misreport unrelated
     * violations, so only this constraint is claimed.
     */
    private static boolean isOverlapViolation(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            String message = cause.getMessage();
            if (message != null && message.contains(OVERLAP_CONSTRAINT)) {
                return true;
            }
            if (cause.getCause() == cause) {
                break;
            }
        }
        return false;
    }

    @Transactional
    public RentalRequest reject(String uid, String bookingId) {
        TenantContext context = requireActiveAdmin(uid);
        RentalRequest request = requireBookingInTenant(context, bookingId);
        applyTransition(request, RentalRequestStatus.REJECTED);
        return request;
    }

    @Transactional
    public RentalRequest complete(String uid, String bookingId) {
        TenantContext context = requireActiveAdmin(uid);
        RentalRequest request = requireBookingInTenant(context, bookingId);
        applyTransition(request, RentalRequestStatus.COMPLETED);
        return request;
    }

    private void applyTransition(RentalRequest request, RentalRequestStatus target) {
        RentalRequestStatus from = request.getStatus();
        if (!request.transitionTo(target)) {
            throw BookingException.illegalTransition(from.getStorageValue(), target.getStorageValue());
        }
    }

    private TenantContext requireActiveAdmin(String uid) {
        TenantContext context = tenantContextService.resolve(uid);
        if (!context.isActiveAdmin() || !context.hasTenantScope()) {
            throw BookingException.notAllowedToBook("Only an active agency admin may do this");
        }
        return context;
    }

    private Car requireCarInTenant(TenantContext context, String carId) {
        if (!context.hasTenantScope()) {
            throw BookingException.carNotFound();
        }
        return carRepository.findByIdAndCompanyId(carId, context.companyId())
                .orElseThrow(BookingException::carNotFound);
    }

    private RentalRequest requireBookingInTenant(TenantContext context, String bookingId) {
        return rentalRequestRepository.findByIdAndCompanyId(bookingId, context.companyId())
                .orElseThrow(BookingException::bookingNotFound);
    }

    /**
     * Names the specific reason so the client can route the renter somewhere
     * useful -- to verification, or to support -- instead of showing one
     * undifferentiated "not allowed".
     */
    private static String bookingRefusalReason(TenantContext context) {
        if (!context.provisioned() || !context.hasTenantScope()) {
            return "This account is not linked to an agency yet";
        }
        if (!context.companyStatus().isOperational()) {
            return "This agency is not currently active";
        }
        if (!context.verificationStatus().allowsBooking()) {
            return "Identity verification must be approved before booking";
        }
        return "This account may not create bookings";
    }
}
