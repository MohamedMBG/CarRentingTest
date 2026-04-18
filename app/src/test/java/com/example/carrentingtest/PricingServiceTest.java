package com.example.carrentingtest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.example.carrentingtest.models.Car;
import com.example.carrentingtest.models.PricingBreakdown;
import com.example.carrentingtest.models.RentalRequest;
import com.example.carrentingtest.pricing.PricingService;

import org.junit.Test;

import java.util.Date;

public class PricingServiceTest {

    @Test
    public void quote_persistsAuthoritativeBreakdown() {
        Car car = new Car();
        car.setPricePerDay(450d);

        Date start = new Date(0L);
        Date end = new Date(2L * 24L * 60L * 60L * 1000L);

        PricingBreakdown breakdown = PricingService.quote(car, start, end);
        assertNotNull(breakdown);
        assertEquals(2, breakdown.getRentalDays());
        assertEquals(900d, breakdown.getBasePrice(), 0.001d);
        assertEquals(900d, breakdown.getTotalPrice(), 0.001d);

        RentalRequest request = new RentalRequest();
        PricingService.applyPricing(request, breakdown);

        assertEquals(900d, request.getTotalPrice(), 0.001d);
        assertEquals(900d, PricingService.getStoredTotal(request), 0.001d);
        assertNotNull(request.getPricingBreakdown());
    }

    @Test
    public void quote_rejectsInvalidRanges() {
        Car car = new Car();
        car.setPricePerDay(450d);

        Date start = new Date(10_000L);
        Date end = new Date(5_000L);

        assertNull(PricingService.quote(car, start, end));
    }
}
