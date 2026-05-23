package com.example.flight_seat_reservation_system.service;

import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.flight_seat_reservation_system.config.BookingProperties;
import com.example.flight_seat_reservation_system.dto.BookingResponse;
import com.example.flight_seat_reservation_system.dto.CreateBookingRequest;
import com.example.flight_seat_reservation_system.entity.Booking;
import com.example.flight_seat_reservation_system.entity.BookingStatus;
import com.example.flight_seat_reservation_system.entity.Flight;
import com.example.flight_seat_reservation_system.entity.FlightStatus;
import com.example.flight_seat_reservation_system.entity.Seat;
import com.example.flight_seat_reservation_system.exception.ConflictException;
import com.example.flight_seat_reservation_system.mapper.BookingMapper;
import com.example.flight_seat_reservation_system.repository.BookingRepository;
import com.example.flight_seat_reservation_system.repository.FlightRepository;
import com.example.flight_seat_reservation_system.repository.SeatRepository;
import com.example.flight_seat_reservation_system.service.validation.BookingWindowValidator;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private FlightRepository flightRepository;
    @Mock
    private SeatRepository seatRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private BookingMapper bookingMapper;
    @Mock
    private BookingWindowValidator bookingWindowValidator;

    private BookingProperties bookingProperties;
    private Clock fixedClock;

    @InjectMocks
    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        bookingProperties = new BookingProperties();
        fixedClock = Clock.fixed(Instant.parse("2026-06-01T10:00:00Z"), ZoneOffset.UTC);
        bookingService = new BookingService(
                flightRepository,
                seatRepository,
                bookingRepository,
                bookingMapper,
                bookingWindowValidator,
                bookingProperties,
                fixedClock
        );
    }

    @Test
    void shouldCreateHeldBooking() {
        Flight flight = activeFlight();
        Seat seat = seat(flight);
        Booking saved = Instancio.of(Booking.class)
                .set(field(Booking::getStatus), BookingStatus.HELD)
                .create();
        BookingResponse mapped = Instancio.of(BookingResponse.class)
                .set(field(BookingResponse::status), "HELD")
                .create();

        when(flightRepository.findById(1L)).thenReturn(Optional.of(flight));
        when(seatRepository.findByFlightIdAndSeatNumberForUpdate(1L, "1A")).thenReturn(Optional.of(seat));
        when(bookingRepository.findByFlightSeatAndStatusesForUpdate(1L, 10L, List.of(BookingStatus.HELD)))
                .thenReturn(List.of());
        when(bookingRepository.findByFlightSeatAndStatusesForUpdate(1L, 10L, List.of(BookingStatus.HELD, BookingStatus.CONFIRMED)))
                .thenReturn(List.of());
        when(bookingRepository.saveAndFlush(any(Booking.class))).thenReturn(saved);
        when(bookingMapper.toResponse(saved)).thenReturn(mapped);

        BookingResponse response = bookingService.createBooking(1L, createBookingRequest());

        assertEquals("HELD", response.status());
        verify(bookingWindowValidator).validateBookingAllowed(flight, fixedClock.instant());
    }

    @Test
    void shouldRejectBookingOnRemovedFlight() {
        Flight flight = activeFlight();
        flight.setStatus(FlightStatus.REMOVED);
        when(flightRepository.findById(1L)).thenReturn(Optional.of(flight));

        assertThrows(ConflictException.class,
                () -> bookingService.createBooking(1L, createBookingRequest()));

        verify(seatRepository, never()).findByFlightIdAndSeatNumberForUpdate(any(), any());
    }

    @Test
    void shouldRejectWhenSeatAlreadyHeld() {
        Flight flight = activeFlight();
        Seat seat = seat(flight);
        Booking activeHold = Instancio.of(Booking.class)
                .set(field(Booking::getStatus), BookingStatus.HELD)
                .set(field(Booking::getHoldExpiresAt), fixedClock.instant().plusSeconds(60))
                .create();

        when(flightRepository.findById(1L)).thenReturn(Optional.of(flight));
        when(seatRepository.findByFlightIdAndSeatNumberForUpdate(1L, "1A")).thenReturn(Optional.of(seat));
        when(bookingRepository.findByFlightSeatAndStatusesForUpdate(1L, 10L, List.of(BookingStatus.HELD)))
                .thenReturn(List.of(activeHold));
        when(bookingRepository.findByFlightSeatAndStatusesForUpdate(1L, 10L, List.of(BookingStatus.HELD, BookingStatus.CONFIRMED)))
                .thenReturn(List.of(activeHold));

        assertThrows(ConflictException.class,
                () -> bookingService.createBooking(1L, createBookingRequest()));
    }

    @Test
    void shouldConfirmHeldBooking() {
        Flight flight = activeFlight();
        Booking booking = Instancio.of(Booking.class)
                .set(field(Booking::getStatus), BookingStatus.HELD)
                .set(field(Booking::getFlight), flight)
                .set(field(Booking::getHoldExpiresAt), fixedClock.instant().plusSeconds(60))
                .create();
        BookingResponse mapped = Instancio.of(BookingResponse.class)
                .set(field(BookingResponse::status), "CONFIRMED")
                .create();

        when(bookingRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(booking));
        when(bookingMapper.toResponse(booking)).thenReturn(mapped);

        BookingResponse response = bookingService.confirmBooking(1L);

        assertEquals("CONFIRMED", response.status());
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
    }

    @Test
    void shouldFailToConfirmExpiredBooking() {
        Flight flight = activeFlight();
        Booking booking = Instancio.of(Booking.class)
                .set(field(Booking::getStatus), BookingStatus.HELD)
                .set(field(Booking::getFlight), flight)
                .set(field(Booking::getHoldExpiresAt), fixedClock.instant().minusSeconds(1))
                .create();

        when(bookingRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(booking));

        assertThrows(ConflictException.class, () -> bookingService.confirmBooking(1L));
        assertEquals(BookingStatus.EXPIRED, booking.getStatus());
    }

    @Test
    void shouldCancelConfirmedBooking() {
        Flight flight = activeFlight();
        Booking booking = Instancio.of(Booking.class)
                .set(field(Booking::getStatus), BookingStatus.CONFIRMED)
                .set(field(Booking::getFlight), flight)
                .create();

        when(bookingRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(booking));

        bookingService.cancelBooking(1L);

        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
    }

    private Flight activeFlight() {
        return Instancio.of(Flight.class)
                .set(field(Flight::getId), 1L)
                .set(field(Flight::getFlightNumber), "FL-" + Instancio.create(Integer.class))
                .set(field(Flight::getStatus), FlightStatus.ACTIVE)
                .create();
    }

    private Seat seat(Flight flight) {
        return Instancio.of(Seat.class)
                .set(field(Seat::getId), 10L)
                .set(field(Seat::getFlight), flight)
                .set(field(Seat::getSeatNumber), "1A")
                .create();
    }

    private CreateBookingRequest createBookingRequest() {
        return Instancio.of(CreateBookingRequest.class)
                .set(field(CreateBookingRequest::seatNumber), "1A")
                .supply(field(CreateBookingRequest::passengerName), () -> "Passenger-" + UUID.randomUUID())
                .supply(field(CreateBookingRequest::passengerEmail), () -> "user-" + UUID.randomUUID() + "@example.com")
                .create();
    }
}
