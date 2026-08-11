package com.bbluxurycars.backend.web.dto;

import com.bbluxurycars.backend.domain.Car;

import java.math.BigDecimal;

/**
 * Wire shape of a fleet entry.
 *
 * <p>{@code companyId} is not emitted: the caller only ever sees their own
 * tenant's fleet, so the field would carry no information and echoing tenant
 * identifiers back to clients invites treating them as inputs.
 */
public record CarResponse(
        String id,
        String model,
        String type,
        BigDecimal pricePerDay,
        String currency,
        Integer seats,
        String transmissionType,
        String imageUrl,
        boolean bookable) {

    public static CarResponse from(Car car, String currency) {
        return new CarResponse(
                car.getId(),
                car.getModel(),
                car.getType(),
                car.getPricePerDay(),
                currency,
                car.getSeats(),
                car.getTransmissionType(),
                car.getImageUrl(),
                car.isBookable());
    }
}
