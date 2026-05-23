package com.example.flight_seat_reservation_system.dto;

import java.time.Instant;
import java.time.LocalDateTime;

public record FlightResponse(
        Long id,
        String flightNumber,
        String originAirportCode,
        String originCity,
        String originTimezone,
        String destinationAirportCode,
        String destinationCity,
        String destinationTimezone,
        LocalDateTime departureTimeLocal,
        Instant departureTimeUtc,
        String status,
        long totalSeats,
        long availableSeats) {
}
