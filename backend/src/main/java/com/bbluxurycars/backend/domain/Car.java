package com.bbluxurycars.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * A vehicle in one agency's fleet.
 *
 * <p>{@code pricePerDay} living here is the point of the whole slice: it is the
 * only price the server will use. The client may send a quote it computed, but
 * the booking is priced from this column (see {@code PricingService}), so a
 * modified app cannot book a car for a price it made up.
 *
 * <p>Only the fields the server needs to price and gate a booking are mirrored.
 * The image gallery stays in Firestore, which the app already reads live; a
 * single {@code imageUrl} is carried so an API-only consumer -- the Phase 4 web
 * dashboard -- can render a list without a second data source.
 */
@Entity
@Table(name = "car")
public class Car implements TenantScoped {

    @Id
    @Column(name = "id", nullable = false, length = 128)
    private String id;

    @Column(name = "company_id", nullable = false, length = 128)
    private String companyId;

    @Column(name = "model", nullable = false, length = 255)
    private String model;

    @Column(name = "type", length = 64)
    private String type;

    @Column(name = "price_per_day", nullable = false, precision = 12, scale = 2)
    private BigDecimal pricePerDay;

    @Column(name = "seats")
    private Integer seats;

    @Column(name = "transmission_type", length = 32)
    private String transmissionType;

    @Column(name = "image_url", length = 1024)
    private String imageUrl;

    @Column(name = "available", nullable = false)
    private boolean available = true;

    @Column(name = "maintenance", nullable = false)
    private boolean maintenance;

    @Column(name = "rental_count", nullable = false)
    private int rentalCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Car() {
        // Required by JPA.
    }

    public Car(String id, String companyId, String model, BigDecimal pricePerDay) {
        this.id = Objects.requireNonNull(id, "id");
        this.companyId = Objects.requireNonNull(companyId, "companyId");
        this.model = Objects.requireNonNull(model, "model");
        this.pricePerDay = Objects.requireNonNull(pricePerDay, "pricePerDay");
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
     * Whether the vehicle can be booked at all, irrespective of dates. Dates
     * are a separate question answered by the overlap constraint; this is the
     * agency's own on/off switch plus the maintenance flag.
     */
    public boolean isBookable() {
        return available && !maintenance;
    }

    public String getId() {
        return id;
    }

    @Override
    public String getCompanyId() {
        return companyId;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = Objects.requireNonNull(model, "model");
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BigDecimal getPricePerDay() {
        return pricePerDay;
    }

    public void setPricePerDay(BigDecimal pricePerDay) {
        this.pricePerDay = Objects.requireNonNull(pricePerDay, "pricePerDay");
    }

    public Integer getSeats() {
        return seats;
    }

    public void setSeats(Integer seats) {
        this.seats = seats;
    }

    public String getTransmissionType() {
        return transmissionType;
    }

    public void setTransmissionType(String transmissionType) {
        this.transmissionType = transmissionType;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public boolean isMaintenance() {
        return maintenance;
    }

    public void setMaintenance(boolean maintenance) {
        this.maintenance = maintenance;
    }

    public int getRentalCount() {
        return rentalCount;
    }

    public void setRentalCount(int rentalCount) {
        this.rentalCount = rentalCount;
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
        return other instanceof Car car && Objects.equals(id, car.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
