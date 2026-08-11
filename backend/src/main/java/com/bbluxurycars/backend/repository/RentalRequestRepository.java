package com.bbluxurycars.backend.repository;

import com.bbluxurycars.backend.domain.RentalRequest;
import com.bbluxurycars.backend.domain.RentalRequestStatus;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

/**
 * Booking reads, all tenant-scoped by construction -- see
 * {@link TenantScopedRepository}.
 */
public interface RentalRequestRepository extends TenantScopedRepository<RentalRequest, String> {

    /** The agency's queue, newest first. */
    List<RentalRequest> findAllByCompanyIdOrderByCreatedAtDesc(String companyId);

    /**
     * A renter's own history. Scoped by tenant as well as user even though the
     * uid alone would be unique: a booking is only ever legible in the tenant
     * that owns it, and keeping the predicate uniform means no query in this
     * interface is a counter-example a future one can copy.
     */
    List<RentalRequest> findAllByCompanyIdAndUserIdOrderByCreatedAtDesc(String companyId,
                                                                       String userId);

    /**
     * Bookings that already hold the given vehicle across an overlapping
     * period.
     *
     * <p>Advisory only -- it powers a clear error message before the write is
     * attempted. The write itself is guarded by the exclusion constraint in V2,
     * which is the sole authority: this query cannot see a conflicting row that
     * another transaction has not yet committed.
     *
     * <p>The comparison is half-open ({@code start < existingEnd} and
     * {@code end > existingStart}) to match {@code tstzrange}'s default
     * {@code '[)'} bounds, so back-to-back rentals are not reported as
     * conflicts.
     */
    List<RentalRequest> findAllByCompanyIdAndCarIdAndStatusInAndStartAtLessThanAndEndAtGreaterThan(
            String companyId,
            String carId,
            Collection<RentalRequestStatus> statuses,
            Instant end,
            Instant start);

    RentalRequest save(RentalRequest request);
}
