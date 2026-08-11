package com.bbluxurycars.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * How a booking's total was arrived at: the priced quote, stored alongside the
 * booking it priced.
 *
 * <p>The equivalent client class carries {@code double}s. Here every amount is
 * a {@link BigDecimal} mapped to {@code NUMERIC(12,2)}: these figures are what
 * a renter is charged and what Phase 2 will invoice against, and binary
 * floating point cannot hold 0.01 exactly, so doubles accumulate cents of drift
 * that never reconcile.
 *
 * <p>Embedded rather than a separate table because a breakdown has no life of
 * its own -- it is never queried, updated or deleted apart from its booking.
 *
 * <p>Immutable: a stored breakdown is the record of a decision already taken.
 * Repricing produces a new instance rather than editing history.
 */
@Embeddable
public class PricingBreakdown {

    public static final String DEFAULT_CURRENCY = "MAD";

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "unit_price_per_day", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPricePerDay;

    @Column(name = "rental_days", nullable = false)
    private int rentalDays;

    @Column(name = "base_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal basePrice;

    // Both are always zero today -- the client stubs extras and discounts at
    // zero too. They are persisted rather than assumed so that adding an extra
    // later does not require a migration of historical bookings whose totals
    // would otherwise be unexplainable.
    @Column(name = "extras_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal extrasTotal;

    @Column(name = "discount_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountTotal;

    @Column(name = "total_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    protected PricingBreakdown() {
        // Required by JPA.
    }

    public PricingBreakdown(String currency,
                            BigDecimal unitPricePerDay,
                            int rentalDays,
                            BigDecimal basePrice,
                            BigDecimal extrasTotal,
                            BigDecimal discountTotal,
                            BigDecimal totalPrice) {
        this.currency = Objects.requireNonNull(currency, "currency");
        this.unitPricePerDay = Objects.requireNonNull(unitPricePerDay, "unitPricePerDay");
        this.rentalDays = rentalDays;
        this.basePrice = Objects.requireNonNull(basePrice, "basePrice");
        this.extrasTotal = Objects.requireNonNull(extrasTotal, "extrasTotal");
        this.discountTotal = Objects.requireNonNull(discountTotal, "discountTotal");
        this.totalPrice = Objects.requireNonNull(totalPrice, "totalPrice");
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getUnitPricePerDay() {
        return unitPricePerDay;
    }

    public int getRentalDays() {
        return rentalDays;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public BigDecimal getExtrasTotal() {
        return extrasTotal;
    }

    public BigDecimal getDiscountTotal() {
        return discountTotal;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    /**
     * Compares totals with {@code compareTo} rather than {@code equals}: the
     * same amount read back from Postgres carries scale 2 while one just
     * computed may not, and {@code BigDecimal.equals} calls those unequal.
     */
    public boolean hasSameTotalAs(BigDecimal other) {
        return other != null && totalPrice.compareTo(other) == 0;
    }
}
