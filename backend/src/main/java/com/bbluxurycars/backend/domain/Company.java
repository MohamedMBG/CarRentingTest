package com.bbluxurycars.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

/**
 * A tenant: one rental agency.
 *
 * <p>The identifier is the Firestore {@code companies} document id, so a row
 * here and its Firestore counterpart are matched without a lookup table.
 *
 * <p>This is the row Phase 2 hangs billing off -- a subscription belongs to a
 * company, and a failed payment drives {@link #status} to
 * {@link CompanyLifecycleStatus#SUSPENDED} (docs/SAAS_ROADMAP.md).
 */
@Entity
@Table(name = "company")
public class Company {

    @Id
    @Column(name = "id", nullable = false, length = 128)
    private String id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "phone", length = 64)
    private String phone;

    @Column(name = "address", length = 512)
    private String address;

    // Stored as two plain columns rather than a spatial type: the only current
    // use is showing an agency pin in the app. PostGIS would add an extension
    // dependency to every environment for a feature nothing needs yet.
    @Column(name = "location_latitude")
    private Double locationLatitude;

    @Column(name = "location_longitude")
    private Double locationLongitude;

    @Column(name = "status", nullable = false, length = 32)
    private CompanyLifecycleStatus status = CompanyLifecycleStatus.PENDING_REVIEW;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Company() {
        // Required by JPA.
    }

    public Company(String id, String name) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
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
     * Coordinates are set as a pair, mirroring the schema's
     * {@code company_location_complete} constraint. Accepting one without the
     * other would fail at flush time with a constraint violation that names the
     * table rather than the caller.
     */
    public void setLocation(Double latitude, Double longitude) {
        if ((latitude == null) != (longitude == null)) {
            throw new IllegalArgumentException(
                    "Latitude and longitude must be set together or both left null");
        }
        this.locationLatitude = latitude;
        this.locationLongitude = longitude;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = Objects.requireNonNull(name, "name");
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Double getLocationLatitude() {
        return locationLatitude;
    }

    public Double getLocationLongitude() {
        return locationLongitude;
    }

    public CompanyLifecycleStatus getStatus() {
        return status;
    }

    public void setStatus(CompanyLifecycleStatus status) {
        this.status = Objects.requireNonNull(status, "status");
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    // Identity is the assigned Firestore id, which is present before persist
    // and never changes, so equality is safe to base on it.
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof Company company && Objects.equals(id, company.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
