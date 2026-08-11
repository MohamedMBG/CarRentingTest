package com.bbluxurycars.backend.booking;

import com.bbluxurycars.backend.error.ApiException;
import org.springframework.http.HttpStatus;

/**
 * A booking the server refuses.
 *
 * <p>Carries the status and code machinery of {@link ApiException}; what lives
 * here is the vocabulary of booking-specific refusals, so that every place a
 * booking can be declined names one of a known set rather than inventing a
 * message.
 */
public class BookingException extends ApiException {

    public BookingException(HttpStatus status, String code, String message) {
        super(status, code, message);
    }

    /**
     * The caller may not book at all: unverified, suspended, not a client, or
     * in a tenant that is not operational.
     *
     * <p>403 rather than 401: the caller's token was valid and their identity
     * is known, so re-authenticating would not help.
     */
    public static BookingException notAllowedToBook(String reason) {
        return new BookingException(HttpStatus.FORBIDDEN, "booking_not_permitted", reason);
    }

    /**
     * The named car does not exist <em>in the caller's tenant</em>. Deliberately
     * indistinguishable from "no such car anywhere", so the endpoint cannot be
     * used to enumerate other agencies' fleets.
     */
    public static BookingException carNotFound() {
        return new BookingException(HttpStatus.NOT_FOUND, "car_not_found", "No such car");
    }

    public static BookingException bookingNotFound() {
        return new BookingException(HttpStatus.NOT_FOUND, "booking_not_found", "No such booking");
    }

    public static BookingException carNotBookable() {
        return new BookingException(HttpStatus.CONFLICT, "car_not_bookable",
                "This vehicle is currently unavailable");
    }

    public static BookingException datesAlreadyHeld() {
        return new BookingException(HttpStatus.CONFLICT, "dates_already_held",
                "This vehicle is already booked for part of that period");
    }

    public static BookingException illegalTransition(String from, String to) {
        return new BookingException(HttpStatus.CONFLICT, "illegal_status_transition",
                "A booking cannot go from " + from + " to " + to);
    }
}
