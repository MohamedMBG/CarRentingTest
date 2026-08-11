package com.bbluxurycars.backend.pricing;

import com.bbluxurycars.backend.domain.Car;
import com.bbluxurycars.backend.domain.PricingBreakdown;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;

/**
 * The server's price for a rental. Authoritative: the client may compute the
 * same figure to display a quote, but this is the one written to the booking
 * (docs/SAAS_ROADMAP.md 3.3).
 *
 * <p>The formula deliberately matches the Android {@code PricingService} --
 * chargeable days are the rental duration rounded <em>up</em>, never fewer than
 * one -- so an honest client's displayed quote equals what it is charged. A
 * server that priced differently would show every renter a surprise at
 * checkout, which is worse than the tamper risk it fixes.
 *
 * <p>Amounts are {@link BigDecimal} at scale 2, half-up, matching the
 * {@code NUMERIC(12,2)} columns. Rounding once at each stored figure keeps
 * base + extras - discount = total exactly true in the database rather than
 * approximately true.
 */
@Service
public class PricingService {

    private static final int MONEY_SCALE = 2;
    private static final RoundingMode MONEY_ROUNDING = RoundingMode.HALF_UP;

    public PricingBreakdown quote(Car car, Instant startAt, Instant endAt) {
        if (car == null) {
            throw new IllegalArgumentException("car is required");
        }
        return quote(car.getPricePerDay(), startAt, endAt);
    }

    public PricingBreakdown quote(BigDecimal pricePerDay, Instant startAt, Instant endAt) {
        if (pricePerDay == null) {
            throw new IllegalArgumentException("pricePerDay is required");
        }
        int rentalDays = chargeableDays(startAt, endAt);

        BigDecimal unitPricePerDay = money(pricePerDay);
        BigDecimal basePrice = money(unitPricePerDay.multiply(BigDecimal.valueOf(rentalDays)));
        BigDecimal extrasTotal = money(BigDecimal.ZERO);
        BigDecimal discountTotal = money(BigDecimal.ZERO);
        BigDecimal totalPrice = money(basePrice.add(extrasTotal).subtract(discountTotal));

        return new PricingBreakdown(
                PricingBreakdown.DEFAULT_CURRENCY,
                unitPricePerDay,
                rentalDays,
                basePrice,
                extrasTotal,
                discountTotal,
                totalPrice);
    }

    /**
     * Chargeable days for a period: any started day counts in full, and a
     * period shorter than a day still costs one.
     *
     * @throws InvalidRentalPeriodException when the period is absent or does
     *         not run forwards. Refusing rather than returning zero days keeps
     *         a nonsensical period from being priced at zero and booked free.
     */
    public int chargeableDays(Instant startAt, Instant endAt) {
        if (startAt == null || endAt == null) {
            throw new InvalidRentalPeriodException("Both a start and an end date are required");
        }
        if (!startAt.isBefore(endAt)) {
            throw new InvalidRentalPeriodException("The end date must be after the start date");
        }

        Duration duration = Duration.between(startAt, endAt);
        long wholeDays = duration.toDays();
        boolean hasPartialDay = !duration.minusDays(wholeDays).isZero();
        long days = hasPartialDay ? wholeDays + 1 : wholeDays;
        return (int) Math.max(1L, days);
    }

    private static BigDecimal money(BigDecimal value) {
        BigDecimal scaled = value.setScale(MONEY_SCALE, MONEY_ROUNDING);
        // A negative price is not a discount to honour, it is bad data: it
        // would credit the renter. Clamp rather than propagate.
        return scaled.signum() < 0 ? BigDecimal.ZERO.setScale(MONEY_SCALE) : scaled;
    }
}
