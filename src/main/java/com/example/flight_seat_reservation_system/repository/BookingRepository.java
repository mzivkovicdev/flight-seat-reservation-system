package com.example.flight_seat_reservation_system.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.flight_seat_reservation_system.entity.Booking;
import com.example.flight_seat_reservation_system.entity.BookingStatus;

import jakarta.persistence.LockModeType;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT b
            FROM Booking b
            JOIN FETCH b.flight
            JOIN FETCH b.seat
            WHERE b.id = :bookingId
            """)
    Optional<Booking> findByIdForUpdate(@Param("bookingId") Long bookingId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT b
            FROM Booking b
            WHERE b.flight.id = :flightId
              AND b.seat.id = :seatId
              AND b.status IN :statuses
            ORDER BY b.createdAt DESC
            """)
    List<Booking> findByFlightSeatAndStatusesForUpdate(
            @Param("flightId") Long flightId,
            @Param("seatId") Long seatId,
            @Param("statuses") List<BookingStatus> statuses);

    @Modifying
    @Query("""
            UPDATE Booking b
            SET b.status = com.example.flight_seat_reservation_system.entity.BookingStatus.EXPIRED
            WHERE b.status = com.example.flight_seat_reservation_system.entity.BookingStatus.HELD
              AND b.holdExpiresAt <= :now
            """)
    int expireHeldBookings(@Param("now") Instant now);
}
