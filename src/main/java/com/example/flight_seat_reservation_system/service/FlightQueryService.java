package com.example.flight_seat_reservation_system.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.flight_seat_reservation_system.dto.FlightResponse;
import com.example.flight_seat_reservation_system.dto.FlightSearchResponse;
import com.example.flight_seat_reservation_system.mapper.FlightMapper;
import com.example.flight_seat_reservation_system.repository.FlightRepository;

@Service
public class FlightQueryService {

    private final FlightRepository flightRepository;
    private final FlightMapper flightMapper;
    private final Clock clock;

    public FlightQueryService(FlightRepository flightRepository, FlightMapper flightMapper, Clock clock) {
        this.flightRepository = flightRepository;
        this.flightMapper = flightMapper;
        this.clock = clock;
    }

    /**
     * Returns active flights with computed seat availability while avoiding N+1 loading.
     */
    @Transactional(readOnly = true)
    public FlightSearchResponse searchFlights(LocalDate date, String origin, String destination) {
        Instant now = clock.instant();
        List<FlightResponse> results = flightRepository.searchFlights(date, origin, destination, now)
                .stream()
                .map(flightMapper::toResponse)
                .toList();
        return new FlightSearchResponse(results);
    }
}
