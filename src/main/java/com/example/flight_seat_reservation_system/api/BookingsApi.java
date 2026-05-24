package com.example.flight_seat_reservation_system.api;

import jakarta.validation.constraints.Positive;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.flight_seat_reservation_system.dto.BookingResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Validated
@RequestMapping
@Tag(name = "Bookings")
public interface BookingsApi {

    @PostMapping("/bookings/{id}/confirm")
    @Operation(summary = "Confirm a held booking")
    ResponseEntity<BookingResponse> confirmBooking(@PathVariable("id") @Positive Long bookingId);

    @DeleteMapping("/bookings/{id}")
    @Operation(summary = "Cancel a booking")
    ResponseEntity<Void> cancelBooking(@PathVariable("id") @Positive Long bookingId);
}
