package com.example.flight_seat_reservation_system.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.flight_seat_reservation_system.entity.Seat;

import jakarta.persistence.LockModeType;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT s
            FROM Seat s
            JOIN FETCH s.flight f
            WHERE f.id = :flightId AND s.seatNumber = :seatNumber
            """)
    Optional<Seat> findByFlightIdAndSeatNumberForUpdate(@Param("flightId") Long flightId,
                                                         @Param("seatNumber") String seatNumber);
}
