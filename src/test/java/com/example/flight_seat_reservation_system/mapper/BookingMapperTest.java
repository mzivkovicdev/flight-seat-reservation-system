package com.example.flight_seat_reservation_system.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import com.example.flight_seat_reservation_system.entity.Booking;
import com.example.flight_seat_reservation_system.entity.BookingStatus;
import com.example.flight_seat_reservation_system.entity.Flight;
import com.example.flight_seat_reservation_system.entity.Seat;

class BookingMapperTest {

    private final BookingMapper mapper = Mappers.getMapper(BookingMapper.class);

    @Test
    void shouldMapNestedFields() {
        Flight flight = new Flight();
        flight.setId(7L);
        flight.setFlightNumber("LH500");

        Seat seat = new Seat();
        seat.setSeatNumber("2C");

        Booking booking = new Booking();
        booking.setId(11L);
        booking.setFlight(flight);
        booking.setSeat(seat);
        booking.setPassengerName("Ana");
        booking.setPassengerEmail("ana@example.com");
        booking.setStatus(BookingStatus.HELD);

        var response = mapper.toResponse(booking);

        assertEquals(7L, response.flightId());
        assertEquals("LH500", response.flightNumber());
        assertEquals("2C", response.seatNumber());
        assertEquals("HELD", response.status());
    }
}
