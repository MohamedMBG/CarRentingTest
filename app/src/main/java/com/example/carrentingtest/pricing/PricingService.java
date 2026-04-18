package com.example.carrentingtest.pricing;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.carrentingtest.models.Car;
import com.example.carrentingtest.models.PricingBreakdown;
import com.example.carrentingtest.models.RentalRequest;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public final class PricingService {
    public static final String DEFAULT_CURRENCY = "MAD";

    private PricingService() {}

    @Nullable
    public static PricingBreakdown quote(@Nullable Car car,
                                         @Nullable Date startDate,
                                         @Nullable Date endDate) {
        if (car == null || startDate == null || endDate == null) {
            return null;
        }

        long durationMillis = endDate.getTime() - startDate.getTime();
        if (durationMillis <= 0) {
            return null;
        }

        int rentalDays = computeRentalDays(startDate, endDate);
        long rentalHours = Math.max(1L, TimeUnit.MILLISECONDS.toHours(durationMillis));
        double unitPricePerDay = sanitizeAmount(car.getPricePerDay());
        double basePrice = sanitizeAmount(unitPricePerDay * rentalDays);
        double extrasTotal = 0d;
        double discountTotal = 0d;
        double totalPrice = sanitizeAmount(basePrice + extrasTotal - discountTotal);

        PricingBreakdown breakdown = new PricingBreakdown();
        breakdown.setUnitPricePerDay(unitPricePerDay);
        breakdown.setRentalDays(rentalDays);
        breakdown.setRentalHours(rentalHours);
        breakdown.setBasePrice(basePrice);
        breakdown.setExtrasTotal(extrasTotal);
        breakdown.setDiscountTotal(discountTotal);
        breakdown.setTotalPrice(totalPrice);
        breakdown.setCurrency(DEFAULT_CURRENCY);
        return breakdown;
    }

    public static int computeRentalDays(@Nullable Date startDate, @Nullable Date endDate) {
        if (startDate == null || endDate == null) {
            return 0;
        }

        long diff = Math.max(0L, endDate.getTime() - startDate.getTime());
        long days = TimeUnit.MILLISECONDS.toDays(diff);
        if (diff % TimeUnit.DAYS.toMillis(1) != 0) {
            days++;
        }
        return (int) Math.max(1L, days);
    }

    public static double getStoredTotal(@Nullable RentalRequest request) {
        if (request == null) {
            return 0d;
        }

        PricingBreakdown breakdown = request.getPricingBreakdown();
        if (breakdown != null) {
            return sanitizeAmount(breakdown.getTotalPrice());
        }

        return sanitizeAmount(request.getTotalPrice());
    }

    public static double sanitizeAmount(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value < 0d) {
            return 0d;
        }
        return value;
    }

    public static void applyPricing(@NonNull RentalRequest request, @Nullable PricingBreakdown breakdown) {
        request.setPricingBreakdown(breakdown);
        request.setTotalPrice(breakdown != null ? sanitizeAmount(breakdown.getTotalPrice()) : 0d);
    }
}
