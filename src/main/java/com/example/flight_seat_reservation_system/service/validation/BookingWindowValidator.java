package com.example.flight_seat_reservation_system.service.validation;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.springframework.stereotype.Component;

import com.example.flight_seat_reservation_system.config.BookingProperties;
import com.example.flight_seat_reservation_system.entity.Flight;
import com.example.flight_seat_reservation_system.exception.ConflictException;

@Component
public class BookingWindowValidator {

    private final BookingProperties bookingProperties;

    public BookingWindowValidator(BookingProperties bookingProperties) {
        this.bookingProperties = bookingProperties;
    }

    /**
     * Enforces the booking cutoff window in the origin timezone of the flight.
     */
    public void validateBookingAllowed(Flight flight, Instant now) {
        ZoneId originZone = ZoneId.of(flight.getOriginTimezone());
        ZonedDateTime nowAtOrigin = now.atZone(originZone);
        ZonedDateTime departureAtOrigin = flight.getDepartureTimeUtc().atZone(originZone);
        ZonedDateTime cutoff = departureAtOrigin.minus(bookingProperties.getCutoffDuration());

        if (!nowAtOrigin.isBefore(cutoff)) {
            throw new ConflictException("Booking window is closed for this flight");
        }
    }
}
