package com.example.flight_seat_reservation_system.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.flight_seat_reservation_system.dto.FlightResponse;
import com.example.flight_seat_reservation_system.mapper.FlightMapper;
import com.example.flight_seat_reservation_system.repository.FlightRepository;
import com.example.flight_seat_reservation_system.repository.FlightSearchProjection;

@ExtendWith(MockitoExtension.class)
class FlightQueryServiceTest {

    @Mock
    private FlightRepository flightRepository;
    @Mock
    private FlightMapper flightMapper;

    private FlightQueryService flightQueryService;
    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(Instant.parse("2026-06-01T10:00:00Z"), ZoneOffset.UTC);
        flightQueryService = new FlightQueryService(flightRepository, flightMapper, fixedClock);
    }

    @Test
    void searchFlights() {
        LocalDate date = LocalDate.parse("2026-08-20");
        FlightSearchProjection projection = org.mockito.Mockito.mock(FlightSearchProjection.class);
        FlightResponse response = Instancio.create(FlightResponse.class);

        when(flightRepository.searchFlights(date, "DUB", "BER", fixedClock.instant())).thenReturn(List.of(projection));
        when(flightMapper.toResponse(projection)).thenReturn(response);

        var result = flightQueryService.searchFlights(date, "DUB", "BER");

        assertEquals(1, result.flights().size());
        assertEquals(response, result.flights().getFirst());
        verify(flightRepository).searchFlights(date, "DUB", "BER", fixedClock.instant());
        verify(flightMapper).toResponse(projection);
        verifyNoMoreInteractions(flightRepository, flightMapper);
    }
}
