package com.bbluxurycars.backend.pricing;

import com.bbluxurycars.backend.domain.PricingBreakdown;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Plain unit tests: pricing touches no database, and keeping it that way is
 * deliberate -- the rule the whole slice rests on should be checkable in
 * milliseconds.
 *
 * <p>The assertions encode the contract with the Android client's own
 * {@code PricingService}: identical day counting, so an honest client's
 * displayed quote is the amount actually charged.
 */
class PricingServiceTest {

    private static final Instant START = Instant.parse("2026-08-01T10:00:00Z");

    private final PricingService pricingService = new PricingService();

    @Test
    void chargesOneDayForExactlyTwentyFourHours() {
        PricingBreakdown quote = pricingService.quote(
                new BigDecimal("450.00"), START, START.plus(Duration.ofHours(24)));

        assertThat(quote.getRentalDays()).isEqualTo(1);
        assertThat(quote.getTotalPrice()).isEqualByComparingTo("450.00");
    }

    /** Any started day is charged in full, matching the client's rounding up. */
    @Test
    void roundsAPartialDayUpToAWholeOne() {
        PricingBreakdown quote = pricingService.quote(
                new BigDecimal("450.00"), START, START.plus(Duration.ofHours(25)));

        assertThat(quote.getRentalDays()).isEqualTo(2);
        assertThat(quote.getTotalPrice()).isEqualByComparingTo("900.00");
    }

    /** A same-morning rental is still a rental, never a free one. */
    @Test
    void chargesAMinimumOfOneDayForAVeryShortRental() {
        PricingBreakdown quote = pricingService.quote(
                new BigDecimal("450.00"), START, START.plus(Duration.ofMinutes(30)));

        assertThat(quote.getRentalDays()).isEqualTo(1);
        assertThat(quote.getTotalPrice()).isEqualByComparingTo("450.00");
    }

    @Test
    void breaksTheTotalDownIntoBaseExtrasAndDiscount() {
        PricingBreakdown quote = pricingService.quote(
                new BigDecimal("199.99"), START, START.plus(Duration.ofDays(3)));

        assertThat(quote.getUnitPricePerDay()).isEqualByComparingTo("199.99");
        assertThat(quote.getRentalDays()).isEqualTo(3);
        assertThat(quote.getBasePrice()).isEqualByComparingTo("599.97");
        assertThat(quote.getExtrasTotal()).isEqualByComparingTo("0.00");
        assertThat(quote.getDiscountTotal()).isEqualByComparingTo("0.00");
        assertThat(quote.getTotalPrice()).isEqualByComparingTo("599.97");
        assertThat(quote.getCurrency()).isEqualTo("MAD");
    }

    /**
     * Amounts are stored at scale 2. A price carrying more precision must be
     * rounded once, here, rather than by the database on write -- otherwise the
     * quote shown and the quote stored can differ by a cent.
     */
    @Test
    void roundsAmountsToTwoDecimalPlacesHalfUp() {
        PricingBreakdown quote = pricingService.quote(
                new BigDecimal("100.005"), START, START.plus(Duration.ofDays(1)));

        assertThat(quote.getUnitPricePerDay()).isEqualByComparingTo("100.01");
        assertThat(quote.getTotalPrice()).isEqualByComparingTo("100.01");
    }

    @Test
    void refusesAPeriodThatDoesNotRunForwards() {
        assertThatThrownBy(() -> pricingService.quote(
                new BigDecimal("450.00"), START, START.minus(Duration.ofHours(1))))
                .isInstanceOf(InvalidRentalPeriodException.class);

        assertThatThrownBy(() -> pricingService.quote(new BigDecimal("450.00"), START, START))
                .isInstanceOf(InvalidRentalPeriodException.class);
    }

    @Test
    void refusesAPeriodWithAMissingDate() {
        assertThatThrownBy(() -> pricingService.quote(new BigDecimal("450.00"), START, null))
                .isInstanceOf(InvalidRentalPeriodException.class);
        assertThatThrownBy(() -> pricingService.quote(new BigDecimal("450.00"), null, START))
                .isInstanceOf(InvalidRentalPeriodException.class);
    }

    /**
     * A negative price is bad data, not a credit. It must never produce a
     * negative total that the renter would be owed.
     */
    @Test
    void clampsANegativePriceToZero() {
        PricingBreakdown quote = pricingService.quote(
                new BigDecimal("-50.00"), START, START.plus(Duration.ofDays(2)));

        assertThat(quote.getTotalPrice()).isEqualByComparingTo("0.00");
    }
}
