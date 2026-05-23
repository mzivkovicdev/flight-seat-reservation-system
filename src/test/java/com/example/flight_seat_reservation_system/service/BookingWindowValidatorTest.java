package com.example.flight_seat_reservation_system.service;

import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.instancio.Instancio;
import org.junit.jupiter.api.Test;

import com.example.flight_seat_reservation_system.config.BookingProperties;
import com.example.flight_seat_reservation_system.entity.Flight;
import com.example.flight_seat_reservation_system.exception.ConflictException;
import com.example.flight_seat_reservation_system.service.validation.BookingWindowValidator;

class BookingWindowValidatorTest {

    @Test
    void validateBookingAllowed() {
        BookingWindowValidator validator = spy(new BookingWindowValidator(new BookingProperties()));

        String originTimezone = "Europe/Dublin";
        Flight flight = Instancio.of(Flight.class)
                .set(field(Flight::getOriginTimezone), originTimezone)
                .set(field(Flight::getDepartureTimeUtc), LocalDateTime.of(2026, 8, 20, 13, 30)
                        .atZone(ZoneId.of(originTimezone))
                        .toInstant())
                .create();

        Instant now = LocalDateTime.of(2026, 8, 20, 12, 44)
                .atZone(ZoneId.of(originTimezone))
                .toInstant();

        assertDoesNotThrow(() -> validator.validateBookingAllowed(flight, now));
        verify(validator).validateBookingAllowed(flight, now);
    }

    @Test
    void validateBookingAllowed_bookingWindowClosed() {
        BookingWindowValidator validator = spy(new BookingWindowValidator(new BookingProperties()));

        String originTimezone = "Europe/Dublin";
        Flight flight = Instancio.of(Flight.class)
                .set(field(Flight::getOriginTimezone), originTimezone)
                .set(field(Flight::getDepartureTimeUtc), LocalDateTime.of(2026, 8, 20, 13, 30)
                        .atZone(ZoneId.of(originTimezone))
                        .toInstant())
                .create();

        Instant now = LocalDateTime.of(2026, 8, 20, 12, 45)
                .atZone(ZoneId.of(originTimezone))
                .toInstant();

        assertThrows(ConflictException.class, () -> validator.validateBookingAllowed(flight, now));
        verify(validator).validateBookingAllowed(flight, now);
    }
}
