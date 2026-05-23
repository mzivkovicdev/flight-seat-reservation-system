package com.example.flight_seat_reservation_system.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import com.example.flight_seat_reservation_system.config.BookingProperties;
import com.example.flight_seat_reservation_system.entity.Flight;
import com.example.flight_seat_reservation_system.exception.ConflictException;
import com.example.flight_seat_reservation_system.service.validation.BookingWindowValidator;

class BookingWindowValidatorTest {

    @Test
    void shouldAllowBookingBeforeCutoffUsingOriginTimezone() {
        BookingProperties properties = new BookingProperties();
        BookingWindowValidator validator = new BookingWindowValidator(properties);

        Flight flight = new Flight();
        flight.setOriginTimezone("Europe/Dublin");
        flight.setDepartureTimeUtc(LocalDateTime.of(2026, 8, 20, 13, 30)
                .atZone(ZoneId.of("Europe/Dublin"))
                .toInstant());

        Instant now = LocalDateTime.of(2026, 8, 20, 12, 44)
                .atZone(ZoneId.of("Europe/Dublin"))
                .toInstant();

        assertDoesNotThrow(() -> validator.validateBookingAllowed(flight, now));
    }

    @Test
    void shouldRejectBookingAtOrAfterCutoffUsingOriginTimezone() {
        BookingProperties properties = new BookingProperties();
        BookingWindowValidator validator = new BookingWindowValidator(properties);

        Flight flight = new Flight();
        flight.setOriginTimezone("Europe/Dublin");
        flight.setDepartureTimeUtc(LocalDateTime.of(2026, 8, 20, 13, 30)
                .atZone(ZoneId.of("Europe/Dublin"))
                .toInstant());

        Instant now = LocalDateTime.of(2026, 8, 20, 12, 45)
                .atZone(ZoneId.of("Europe/Dublin"))
                .toInstant();

        assertThrows(ConflictException.class, () -> validator.validateBookingAllowed(flight, now));
    }
}
