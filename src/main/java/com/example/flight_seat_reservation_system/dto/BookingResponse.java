package com.example.flight_seat_reservation_system.dto;

import java.time.Instant;

public record BookingResponse(
        Long id,
        Long flightId,
        String flightNumber,
        String seatNumber,
        String passengerName,
        String passengerEmail,
        String status,
        Instant holdExpiresAt,
        Instant confirmedAt,
        Instant cancelledAt,
        Instant createdAt) {
}
