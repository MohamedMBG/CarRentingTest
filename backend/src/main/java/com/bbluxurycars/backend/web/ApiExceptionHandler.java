package com.bbluxurycars.backend.web;

import com.bbluxurycars.backend.booking.BookingException;
import com.bbluxurycars.backend.pricing.InvalidRentalPeriodException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Turns domain refusals into a single error shape:
 * {@code {"code": ..., "message": ...}}.
 *
 * <p>A stable {@code code} is what lets the Android client tell "these dates
 * are taken" from "you are not verified" and send the renter somewhere useful.
 * Without it the client would have to match on message text, which cannot be
 * changed or translated afterwards.
 *
 * <p>Nothing here logs or echoes the underlying exception. Constraint names and
 * SQL fragments describe the schema, and an error body is the wrong place to
 * publish it.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    public record ApiError(String code, String message) {
    }

    @ExceptionHandler(BookingException.class)
    public ResponseEntity<ApiError> handleBooking(BookingException e) {
        return ResponseEntity.status(e.getStatus())
                .body(new ApiError(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(InvalidRentalPeriodException.class)
    public ResponseEntity<ApiError> handleInvalidPeriod(InvalidRentalPeriodException e) {
        return ResponseEntity.badRequest()
                .body(new ApiError("invalid_rental_period", e.getMessage()));
    }

    /**
     * Bean-validation failures on a request body. The offending field is named
     * because it is the client's own payload, and a bare "invalid request"
     * leaves an integrator guessing.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse("Request body is invalid");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError("invalid_request", detail));
    }
}
