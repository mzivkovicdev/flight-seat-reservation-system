package com.example.flight_seat_reservation_system.repository;

import java.time.Instant;
import java.time.LocalDateTime;

public interface FlightSearchProjection {

    Long getId();

    String getFlightNumber();

    String getOriginAirportCode();

    String getOriginCity();

    String getOriginTimezone();

    String getDestinationAirportCode();

    String getDestinationCity();

    String getDestinationTimezone();

    LocalDateTime getDepartureTimeLocal();

    Instant getDepartureTimeUtc();

    String getStatus();

    long getTotalSeats();

    long getAvailableSeats();
}
