package com.bbluxurycars.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

/**
 * One renter's booking of one vehicle for one period.
 *
 * <p>State changes go through {@link #transitionTo(RentalRequestStatus)} rather
 * than a public setter, so the state machine in {@link RentalRequestStatus}
 * cannot be bypassed by a caller that simply assigns a field.
 *
 * <p>Overlap is <em>not</em> checked here. It is enforced by the exclusion
 * constraint in V2, because two concurrent approvals can both pass an
 * in-memory check before either commits (see the migration's comment).
 *
 * <p>{@code carId} and {@code userId} are plain columns, not associations: the
 * booking queue lists hundreds of rows and a {@code @ManyToOne} would turn each
 * into a lazy load. The composite foreign keys in V2 keep them honest.
 */
@Entity
@Table(name = "rental_request")
public class RentalRequest implements TenantScoped {

    @Id
    @Column(name = "id", nullable = false, length = 128)
    private String id;

    @Column(name = "company_id", nullable = false, length = 128)
    private String companyId;

    @Column(name = "car_id", nullable = false, length = 128)
    private String carId;

    @Column(name = "user_id", nullable = false, length = 128)
    private String userId;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Column(name = "status", nullable = false, length = 32)
    private RentalRequestStatus status = RentalRequestStatus.PENDING;

    @Column(name = "additional_requests", length = 2000)
    private String additionalRequests;

    @Embedded
    private PricingBreakdown pricing;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected RentalRequest() {
        // Required by JPA.
    }

    public RentalRequest(String id,
                         String companyId,
                         String carId,
                         String userId,
                         Instant startAt,
                         Instant endAt,
                         PricingBreakdown pricing) {
        this.id = Objects.requireNonNull(id, "id");
        this.companyId = Objects.requireNonNull(companyId, "companyId");
        this.carId = Objects.requireNonNull(carId, "carId");
        this.userId = Objects.requireNonNull(userId, "userId");
        this.startAt = Objects.requireNonNull(startAt, "startAt");
        this.endAt = Objects.requireNonNull(endAt, "endAt");
        this.pricing = Objects.requireNonNull(pricing, "pricing");
        if (!startAt.isBefore(endAt)) {
            // Rejected here as well as by the CHECK constraint: a caller gets
            // an error naming the argument rather than one naming the table.
            throw new IllegalArgumentException("startAt must be before endAt");
        }
    }

    @PrePersist
    void onInsert() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    /**
     * Applies a status change if the state machine allows it.
     *
     * <p>{@code completedAt} is maintained here rather than by the caller so it
     * can never contradict the status -- the same invariant the
     * {@code rental_request_completed_at_matches_status} constraint holds in
     * the database.
     *
     * @return true when the transition was applied, false when it is illegal
     */
    public boolean transitionTo(RentalRequestStatus target) {
        if (!status.canTransitionTo(target)) {
            return false;
        }
        this.status = target;
        this.completedAt = target == RentalRequestStatus.COMPLETED ? Instant.now() : null;
        return true;
    }

    public String getId() {
        return id;
    }

    @Override
    public String getCompanyId() {
        return companyId;
    }

    public String getCarId() {
        return carId;
    }

    public String getUserId() {
        return userId;
    }

    public Instant getStartAt() {
        return startAt;
    }

    public Instant getEndAt() {
        return endAt;
    }

    public RentalRequestStatus getStatus() {
        return status;
    }

    public String getAdditionalRequests() {
        return additionalRequests;
    }

    public void setAdditionalRequests(String additionalRequests) {
        this.additionalRequests = additionalRequests;
    }

    public PricingBreakdown getPricing() {
        return pricing;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof RentalRequest request && Objects.equals(id, request.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
