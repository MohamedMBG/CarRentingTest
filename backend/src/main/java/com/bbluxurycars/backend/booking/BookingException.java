package com.bbluxurycars.backend.booking;

import org.springframework.http.HttpStatus;

/**
 * A booking that the server refuses, with the HTTP status the refusal maps to
 * and a stable machine-readable code.
 *
 * <p>The code exists so the Android client can react to a specific refusal --
 * "these dates are taken" deserves a different screen from "your account is not
 * verified" -- without parsing the human-readable message, which is free to
 * change or be translated.
 */
public class BookingException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public BookingException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
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
