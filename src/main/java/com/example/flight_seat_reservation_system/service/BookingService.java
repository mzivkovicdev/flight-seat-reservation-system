package com.example.flight_seat_reservation_system.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.flight_seat_reservation_system.config.BookingProperties;
import com.example.flight_seat_reservation_system.dto.BookingResponse;
import com.example.flight_seat_reservation_system.dto.CreateBookingRequest;
import com.example.flight_seat_reservation_system.entity.Booking;
import com.example.flight_seat_reservation_system.entity.BookingStatus;
import com.example.flight_seat_reservation_system.entity.Flight;
import com.example.flight_seat_reservation_system.entity.FlightStatus;
import com.example.flight_seat_reservation_system.entity.Seat;
import com.example.flight_seat_reservation_system.exception.ConflictException;
import com.example.flight_seat_reservation_system.exception.NotFoundException;
import com.example.flight_seat_reservation_system.mapper.BookingMapper;
import com.example.flight_seat_reservation_system.repository.BookingRepository;
import com.example.flight_seat_reservation_system.repository.FlightRepository;
import com.example.flight_seat_reservation_system.repository.SeatRepository;
import com.example.flight_seat_reservation_system.service.validation.BookingWindowValidator;

@Service
public class BookingService {

    private final FlightRepository flightRepository;
    private final SeatRepository seatRepository;
    private final BookingRepository bookingRepository;
    private final BookingMapper bookingMapper;
    private final BookingWindowValidator bookingWindowValidator;
    private final BookingProperties bookingProperties;
    private final Clock clock;

    public BookingService(FlightRepository flightRepository,
                          SeatRepository seatRepository,
                          BookingRepository bookingRepository,
                          BookingMapper bookingMapper,
                          BookingWindowValidator bookingWindowValidator,
                          BookingProperties bookingProperties,
                          Clock clock) {
        this.flightRepository = flightRepository;
        this.seatRepository = seatRepository;
        this.bookingRepository = bookingRepository;
        this.bookingMapper = bookingMapper;
        this.bookingWindowValidator = bookingWindowValidator;
        this.bookingProperties = bookingProperties;
        this.clock = clock;
    }

    /**
     * Creates a HELD booking if seat is available and booking window is still open.
     *
     * @param flightId flight identifier
     * @param request booking creation payload
     * @return created booking in HELD status
     * @throws NotFoundException if flight or seat does not exist
     * @throws ConflictException if flight is removed, booking window is closed, or seat is unavailable
     */
    @Transactional
    public BookingResponse createBooking(Long flightId, CreateBookingRequest request) {
        Instant now = clock.instant();
        Flight flight = flightRepository.findById(flightId)
                .orElseThrow(() -> new NotFoundException("Flight not found"));

        if (flight.getStatus() == FlightStatus.REMOVED) {
            throw new ConflictException("Flight is removed and no longer bookable");
        }

        bookingWindowValidator.validateBookingAllowed(flight, now);

        String normalizedSeat = request.seatNumber().toUpperCase();
        Seat seat = seatRepository.findByFlightIdAndSeatNumberForUpdate(flightId, normalizedSeat)
                .orElseThrow(() -> new NotFoundException("Seat not found for this flight"));

        expireHeldIfNeeded(flightId, seat.getId(), now);

        List<Booking> activeBookings = bookingRepository.findByFlightSeatAndStatusesForUpdate(
                flightId,
                seat.getId(),
                List.of(BookingStatus.HELD, BookingStatus.CONFIRMED)
        );

        boolean hasConfirmedBooking = activeBookings.stream()
                .anyMatch(active -> active.getStatus() == BookingStatus.CONFIRMED);
        if (hasConfirmedBooking) {
            throw new ConflictException("Seat is already confirmed");
        }

        boolean hasActiveHeldBooking = activeBookings.stream()
                .anyMatch(active -> active.getStatus() == BookingStatus.HELD
                        && active.getHoldExpiresAt() != null
                        && active.getHoldExpiresAt().isAfter(now));
        if (hasActiveHeldBooking) {
            throw new ConflictException("Seat is already held");
        }

        Booking booking = new Booking();
        booking.setFlight(flight);
        booking.setSeat(seat);
        booking.setPassengerName(request.passengerName());
        booking.setPassengerEmail(request.passengerEmail().toLowerCase());
        booking.setStatus(BookingStatus.HELD);
        booking.setHoldExpiresAt(now.plus(bookingProperties.getHoldDuration()));

        try {
            Booking saved = bookingRepository.saveAndFlush(booking);
            return bookingMapper.toResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("Seat is not available");
        }
    }

    /**
     * Confirms a HELD booking when the hold has not expired.
     *
     * @param bookingId booking identifier
     * @return confirmed booking
     * @throws NotFoundException if booking does not exist
     * @throws ConflictException if flight is removed, booking is not HELD, or hold has expired
     */
    @Transactional
    public BookingResponse confirmBooking(Long bookingId) {
        Instant now = clock.instant();
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found"));

        if (booking.getFlight().getStatus() == FlightStatus.REMOVED) {
            throw new ConflictException("Cannot confirm booking for removed flight");
        }

        if (booking.getStatus() != BookingStatus.HELD) {
            throw new ConflictException("Only HELD bookings can be confirmed");
        }

        if (isExpired(booking, now)) {
            booking.setStatus(BookingStatus.EXPIRED);
            throw new ConflictException("Booking hold has expired");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setConfirmedAt(now);
        booking.setHoldExpiresAt(null);

        return bookingMapper.toResponse(booking);
    }

    /**
     * Cancels a HELD or CONFIRMED booking while preserving booking history.
     *
     * @param bookingId booking identifier
     * @throws NotFoundException if booking does not exist
     * @throws ConflictException if booking has expired or is already terminal
     */
    @Transactional
    public void cancelBooking(Long bookingId) {
        Instant now = clock.instant();
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found"));

        if (booking.getStatus() == BookingStatus.HELD && isExpired(booking, now)) {
            booking.setStatus(BookingStatus.EXPIRED);
            throw new ConflictException("Booking already expired");
        }

        if (booking.getStatus() != BookingStatus.HELD && booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new ConflictException("Only HELD or CONFIRMED bookings can be cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(now);
        booking.setHoldExpiresAt(null);
    }

    /**
     * Expires stale HELD bookings in batch.
     *
     * @return number of expired bookings
     */
    @Transactional
    public int expireHeldBookings() {
        return bookingRepository.expireHeldBookings(clock.instant());
    }

    private void expireHeldIfNeeded(Long flightId, Long seatId, Instant now) {
        List<Booking> held = bookingRepository.findByFlightSeatAndStatusesForUpdate(
                flightId,
                seatId,
                List.of(BookingStatus.HELD)
        );
        held.stream()
                .filter(booking -> isExpired(booking, now))
                .forEach(booking -> booking.setStatus(BookingStatus.EXPIRED));
    }

    private boolean isExpired(Booking booking, Instant now) {
        return booking.getHoldExpiresAt() != null && !booking.getHoldExpiresAt().isAfter(now);
    }
}
