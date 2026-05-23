package com.example.flight_seat_reservation_system.controller;

import java.time.LocalDate;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.example.flight_seat_reservation_system.api.FlightsApi;
import com.example.flight_seat_reservation_system.dto.BookingResponse;
import com.example.flight_seat_reservation_system.dto.CreateBookingRequest;
import com.example.flight_seat_reservation_system.dto.FlightSearchResponse;
import com.example.flight_seat_reservation_system.service.BookingService;
import com.example.flight_seat_reservation_system.service.FlightQueryService;

@RestController
public class FlightsController implements FlightsApi {

    private final FlightQueryService flightQueryService;
    private final BookingService bookingService;

    public FlightsController(FlightQueryService flightQueryService, BookingService bookingService) {
        this.flightQueryService = flightQueryService;
        this.bookingService = bookingService;
    }

    @Override
    public ResponseEntity<FlightSearchResponse> getFlights(LocalDate date, String origin, String destination) {
        return ResponseEntity.ok(flightQueryService.searchFlights(date, origin, destination));
    }

    @Override
    public ResponseEntity<BookingResponse> createBooking(Long flightId, CreateBookingRequest request) {
        BookingResponse response = bookingService.createBooking(flightId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
