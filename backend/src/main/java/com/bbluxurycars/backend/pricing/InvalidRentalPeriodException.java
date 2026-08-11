package com.bbluxurycars.backend.pricing;

/**
 * A rental period that cannot be priced: missing dates, or an end that does not
 * follow its start. Surfaced as 400 by {@code ApiExceptionHandler}.
 */
public class InvalidRentalPeriodException extends RuntimeException {

    public InvalidRentalPeriodException(String message) {
        super(message);
    }
}
