package com.example.flight_seat_reservation_system.repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.flight_seat_reservation_system.entity.Flight;
import com.example.flight_seat_reservation_system.entity.FlightStatus;

public interface FlightRepository extends JpaRepository<Flight, Long> {

    Optional<Flight> findByIdAndStatus(Long id, FlightStatus status);

    @Query("""
            SELECT f.id AS id,
                   f.flightNumber AS flightNumber,
                   f.originAirportCode AS originAirportCode,
                   f.originCity AS originCity,
                   f.originTimezone AS originTimezone,
                   f.destinationAirportCode AS destinationAirportCode,
                   f.destinationCity AS destinationCity,
                   f.destinationTimezone AS destinationTimezone,
                   f.departureTimeLocal AS departureTimeLocal,
                   f.departureTimeUtc AS departureTimeUtc,
                   f.status AS status,
                   COUNT(DISTINCT s.id) AS totalSeats,
                   (COUNT(DISTINCT s.id) - COUNT(DISTINCT b.id)) AS availableSeats
            FROM Flight f
            LEFT JOIN Seat s ON s.flight = f
            LEFT JOIN Booking b ON b.flight = f
                AND b.seat = s
                AND (
                    b.status = com.example.flight_seat_reservation_system.entity.BookingStatus.CONFIRMED
                    OR (
                        b.status = com.example.flight_seat_reservation_system.entity.BookingStatus.HELD
                        AND b.holdExpiresAt > :now
                    )
                )
            WHERE f.status = com.example.flight_seat_reservation_system.entity.FlightStatus.ACTIVE
              AND f.departureDateLocal = COALESCE(:date, f.departureDateLocal)
              AND (
                    LOWER(f.originAirportCode) = LOWER(COALESCE(:origin, f.originAirportCode))
                    OR LOWER(f.originCity) LIKE LOWER(CONCAT('%', COALESCE(:origin, ''), '%'))
              )
              AND (
                    LOWER(f.destinationAirportCode) = LOWER(COALESCE(:destination, f.destinationAirportCode))
                    OR LOWER(f.destinationCity) LIKE LOWER(CONCAT('%', COALESCE(:destination, ''), '%'))
              )
            GROUP BY f.id
            ORDER BY f.departureTimeUtc ASC
            """)
    List<FlightSearchProjection> searchFlights(
            @Param("date") LocalDate date,
            @Param("origin") String origin,
            @Param("destination") String destination,
            @Param("now") Instant now);
}
