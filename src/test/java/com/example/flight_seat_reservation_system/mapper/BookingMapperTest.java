package com.example.flight_seat_reservation_system.mapper;

import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import com.example.flight_seat_reservation_system.entity.Booking;
import com.example.flight_seat_reservation_system.entity.BookingStatus;
import com.example.flight_seat_reservation_system.entity.Flight;
import com.example.flight_seat_reservation_system.entity.Seat;

class BookingMapperTest {

    @Test
    void toResponse() {
        BookingMapper mapper = spy(Mappers.getMapper(BookingMapper.class));

        Long flightId = Instancio.create(Long.class);
        String flightNumber = "FL-" + Instancio.create(Integer.class);
        String seatNumber = "2C";
        BookingStatus status = BookingStatus.HELD;

        Flight flight = Instancio.of(Flight.class)
                .set(field(Flight::getId), flightId)
                .set(field(Flight::getFlightNumber), flightNumber)
                .create();

        Seat seat = Instancio.of(Seat.class)
                .set(field(Seat::getSeatNumber), seatNumber)
                .create();

        Booking booking = Instancio.of(Booking.class)
                .set(field(Booking::getFlight), flight)
                .set(field(Booking::getSeat), seat)
                .set(field(Booking::getStatus), status)
                .create();

        var response = mapper.toResponse(booking);

        assertNotNull(response);
        assertEquals(flightId, response.flightId());
        assertEquals(flightNumber, response.flightNumber());
        assertEquals(seatNumber, response.seatNumber());
        assertEquals(status.name(), response.status());
        verify(mapper).toResponse(booking);
    }
}
