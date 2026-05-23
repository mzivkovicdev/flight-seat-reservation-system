package com.example.flight_seat_reservation_system.service;

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
        Booking saved = new Booking();
        saved.setStatus(BookingStatus.HELD);
        BookingResponse mapped = new BookingResponse(1L, 1L, "FR100", "1A", "John", "john@x.com", "HELD", null, null, null,
                Instant.now());

        when(flightRepository.findById(1L)).thenReturn(Optional.of(flight));
        when(seatRepository.findByFlightIdAndSeatNumberForUpdate(1L, "1A")).thenReturn(Optional.of(seat));
        when(bookingRepository.findByFlightSeatAndStatusesForUpdate(1L, 10L, List.of(BookingStatus.HELD)))
                .thenReturn(List.of());
        when(bookingRepository.findByFlightSeatAndStatusesForUpdate(1L, 10L, List.of(BookingStatus.HELD, BookingStatus.CONFIRMED)))
                .thenReturn(List.of());
        when(bookingRepository.saveAndFlush(any(Booking.class))).thenReturn(saved);
        when(bookingMapper.toResponse(saved)).thenReturn(mapped);

        BookingResponse response = bookingService.createBooking(1L,
                new CreateBookingRequest("1A", "John", "john@x.com"));

        assertEquals("HELD", response.status());
        verify(bookingWindowValidator).validateBookingAllowed(flight, fixedClock.instant());
    }

    @Test
    void shouldRejectBookingOnRemovedFlight() {
        Flight flight = activeFlight();
        flight.setStatus(FlightStatus.REMOVED);
        when(flightRepository.findById(1L)).thenReturn(Optional.of(flight));

        assertThrows(ConflictException.class,
                () -> bookingService.createBooking(1L, new CreateBookingRequest("1A", "John", "john@x.com")));

        verify(seatRepository, never()).findByFlightIdAndSeatNumberForUpdate(any(), any());
    }

    @Test
    void shouldRejectWhenSeatAlreadyHeld() {
        Flight flight = activeFlight();
        Seat seat = seat(flight);
        Booking activeHold = new Booking();
        activeHold.setStatus(BookingStatus.HELD);
        activeHold.setHoldExpiresAt(fixedClock.instant().plusSeconds(60));

        when(flightRepository.findById(1L)).thenReturn(Optional.of(flight));
        when(seatRepository.findByFlightIdAndSeatNumberForUpdate(1L, "1A")).thenReturn(Optional.of(seat));
        when(bookingRepository.findByFlightSeatAndStatusesForUpdate(1L, 10L, List.of(BookingStatus.HELD)))
                .thenReturn(List.of(activeHold));
        when(bookingRepository.findByFlightSeatAndStatusesForUpdate(1L, 10L, List.of(BookingStatus.HELD, BookingStatus.CONFIRMED)))
                .thenReturn(List.of(activeHold));

        assertThrows(ConflictException.class,
                () -> bookingService.createBooking(1L, new CreateBookingRequest("1A", "John", "john@x.com")));
    }

    @Test
    void shouldConfirmHeldBooking() {
        Flight flight = activeFlight();
        Booking booking = new Booking();
        booking.setStatus(BookingStatus.HELD);
        booking.setFlight(flight);
        booking.setHoldExpiresAt(fixedClock.instant().plusSeconds(60));
        BookingResponse mapped = new BookingResponse(1L, 1L, "FR100", "1A", "John", "john@x.com", "CONFIRMED", null,
                fixedClock.instant(), null, fixedClock.instant());

        when(bookingRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(booking));
        when(bookingMapper.toResponse(booking)).thenReturn(mapped);

        BookingResponse response = bookingService.confirmBooking(1L);

        assertEquals("CONFIRMED", response.status());
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
    }

    @Test
    void shouldFailToConfirmExpiredBooking() {
        Flight flight = activeFlight();
        Booking booking = new Booking();
        booking.setStatus(BookingStatus.HELD);
        booking.setFlight(flight);
        booking.setHoldExpiresAt(fixedClock.instant().minusSeconds(1));

        when(bookingRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(booking));

        assertThrows(ConflictException.class, () -> bookingService.confirmBooking(1L));
        assertEquals(BookingStatus.EXPIRED, booking.getStatus());
    }

    @Test
    void shouldCancelConfirmedBooking() {
        Flight flight = activeFlight();
        Booking booking = new Booking();
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setFlight(flight);

        when(bookingRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(booking));

        bookingService.cancelBooking(1L);

        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
    }

    private Flight activeFlight() {
        Flight flight = new Flight();
        flight.setId(1L);
        flight.setFlightNumber("FR100");
        flight.setStatus(FlightStatus.ACTIVE);
        return flight;
    }

    private Seat seat(Flight flight) {
        Seat seat = new Seat();
        seat.setId(10L);
        seat.setFlight(flight);
        seat.setSeatNumber("1A");
        return seat;
    }
}
