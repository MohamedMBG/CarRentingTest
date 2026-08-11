package com.bbluxurycars.backend.web;

import com.bbluxurycars.backend.booking.BookingService;
import com.bbluxurycars.backend.domain.PricingBreakdown;
import com.bbluxurycars.backend.domain.RentalRequest;
import com.bbluxurycars.backend.security.FirebaseAuthFilter;
import com.bbluxurycars.backend.web.dto.BookingResponse;
import com.bbluxurycars.backend.web.dto.CreateBookingRequest;
import com.bbluxurycars.backend.web.dto.QuoteRequest;
import com.bbluxurycars.backend.web.dto.QuoteResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Booking as the server sees it: quote, create, and the admin transitions.
 *
 * <p>The uid always comes from the request attribute {@link FirebaseAuthFilter}
 * set after verifying the ID token -- never from a body field -- so no endpoint
 * here can act for another user or another tenant.
 */
@RestController
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /**
     * POST rather than GET despite reading nothing: the period and car travel
     * as a JSON body with typed instants, and a quote is cheap but not
     * cacheable -- the price follows the car row, which an admin can change at
     * any moment.
     */
    @PostMapping("/v1/bookings/quote")
    public QuoteResponse quote(HttpServletRequest request, @Valid @RequestBody QuoteRequest body) {
        PricingBreakdown pricing = bookingService.quote(
                uidOf(request), body.carId(), body.startAt(), body.endAt());
        return QuoteResponse.from(pricing);
    }

    @PostMapping("/v1/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse create(HttpServletRequest request,
                                  @Valid @RequestBody CreateBookingRequest body) {
        RentalRequest created = bookingService.create(
                uidOf(request),
                body.carId(),
                body.startAt(),
                body.endAt(),
                body.additionalRequests());
        return BookingResponse.from(created);
    }

    /** The tenant's queue for an admin; the caller's own bookings otherwise. */
    @GetMapping("/v1/bookings")
    public List<BookingResponse> list(HttpServletRequest request) {
        return bookingService.listForCaller(uidOf(request)).stream()
                .map(BookingResponse::from)
                .toList();
    }

    // Transitions are separate endpoints rather than one PATCH of a status
    // field: each is a distinct authorised action, and a status field a client
    // can set to any value is exactly the pattern this slice removes.
    @PostMapping("/v1/bookings/{id}/approve")
    public BookingResponse approve(HttpServletRequest request, @PathVariable String id) {
        return BookingResponse.from(bookingService.approve(uidOf(request), id));
    }

    @PostMapping("/v1/bookings/{id}/reject")
    public BookingResponse reject(HttpServletRequest request, @PathVariable String id) {
        return BookingResponse.from(bookingService.reject(uidOf(request), id));
    }

    @PostMapping("/v1/bookings/{id}/complete")
    public BookingResponse complete(HttpServletRequest request, @PathVariable String id) {
        return BookingResponse.from(bookingService.complete(uidOf(request), id));
    }

    private static String uidOf(HttpServletRequest request) {
        return (String) request.getAttribute(FirebaseAuthFilter.UID_ATTRIBUTE);
    }
}
