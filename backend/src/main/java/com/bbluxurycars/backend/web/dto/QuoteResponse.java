package com.bbluxurycars.backend.web.dto;

import com.bbluxurycars.backend.domain.PricingBreakdown;

import java.math.BigDecimal;

/**
 * Wire shape of a price quote. Mirrors the client's {@code PricingBreakdown}
 * field for field so the Android app can display a server quote through the
 * screens it already has.
 */
public record QuoteResponse(
        String currency,
        BigDecimal unitPricePerDay,
        int rentalDays,
        BigDecimal basePrice,
        BigDecimal extrasTotal,
        BigDecimal discountTotal,
        BigDecimal totalPrice) {

    public static QuoteResponse from(PricingBreakdown pricing) {
        return new QuoteResponse(
                pricing.getCurrency(),
                pricing.getUnitPricePerDay(),
                pricing.getRentalDays(),
                pricing.getBasePrice(),
                pricing.getExtrasTotal(),
                pricing.getDiscountTotal(),
                pricing.getTotalPrice());
    }
}
