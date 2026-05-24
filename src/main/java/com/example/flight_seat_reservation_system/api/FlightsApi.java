package com.example.flight_seat_reservation_system.api;

import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.flight_seat_reservation_system.dto.BookingResponse;
import com.example.flight_seat_reservation_system.dto.CreateBookingRequest;
import com.example.flight_seat_reservation_system.dto.FlightSearchResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Validated
@RequestMapping
@Tag(name = "Flights")
public interface FlightsApi {

    @GetMapping("/flights")
    @Operation(summary = "Search flights")
    ResponseEntity<FlightSearchResponse> getFlights(
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) String origin,
            @RequestParam(required = false) String destination);

    @PostMapping("/flights/{id}/bookings")
    @Operation(summary = "Create a held booking")
    ResponseEntity<BookingResponse> createBooking(
            @PathVariable("id") @Positive Long flightId,
            @Valid @RequestBody CreateBookingRequest request);
}
