package com.example.flight_seat_reservation_system.service;

import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.flight_seat_reservation_system.dto.CreateFlightRequest;
import com.example.flight_seat_reservation_system.dto.FlightResponse;
import com.example.flight_seat_reservation_system.entity.Flight;
import com.example.flight_seat_reservation_system.entity.FlightStatus;
import com.example.flight_seat_reservation_system.exception.NotFoundException;
import com.example.flight_seat_reservation_system.mapper.CreateFlightMapper;
import com.example.flight_seat_reservation_system.mapper.FlightMapper;
import com.example.flight_seat_reservation_system.repository.FlightRepository;
import com.example.flight_seat_reservation_system.service.validation.SeatNumberGenerator;

@ExtendWith(MockitoExtension.class)
class AdminFlightServiceTest {

    @Mock
    private FlightRepository flightRepository;
    @Mock
    private CreateFlightMapper createFlightMapper;
    @Mock
    private FlightMapper flightMapper;
    @Mock
    private SeatNumberGenerator seatNumberGenerator;

    private AdminFlightService adminFlightService;

    @BeforeEach
    void setUp() {
        adminFlightService = new AdminFlightService(flightRepository, createFlightMapper, flightMapper, seatNumberGenerator);
    }

    @Test
    void createFlight() {
        LocalDateTime departureLocal = LocalDateTime.parse("2026-08-20T14:30:00");
        CreateFlightRequest request = Instancio.of(CreateFlightRequest.class)
                .set(field(CreateFlightRequest::originTimezone), "Europe/Dublin")
                .set(field(CreateFlightRequest::departureTimeLocal), departureLocal)
                .set(field(CreateFlightRequest::seatCount), 3)
                .create();

        Flight mappedFlight = Instancio.create(Flight.class);
        mappedFlight.setSeats(new java.util.ArrayList<>());
        Flight savedFlight = mappedFlight;
        FlightResponse mappedResponse = Instancio.create(FlightResponse.class);

        when(createFlightMapper.toEntity(request)).thenReturn(mappedFlight);
        when(seatNumberGenerator.generate(3)).thenReturn(List.of("1A", "1B", "1C"));
        when(flightRepository.save(mappedFlight)).thenReturn(savedFlight);
        when(flightMapper.toResponse(savedFlight, 3L, 3L)).thenReturn(mappedResponse);

        FlightResponse response = adminFlightService.createFlight(request);

        assertEquals(mappedResponse, response);
        assertEquals(FlightStatus.ACTIVE, mappedFlight.getStatus());
        assertEquals(LocalDate.from(departureLocal), mappedFlight.getDepartureDateLocal());
        verify(seatNumberGenerator).generate(3);
        verify(flightMapper).toResponse(savedFlight, 3L, 3L);
        verifyNoMoreInteractions(flightMapper);
    }

    @Test
    void removeFlight() {
        Flight flight = Instancio.of(Flight.class)
                .set(field(Flight::getStatus), FlightStatus.ACTIVE)
                .create();
        when(flightRepository.findById(1L)).thenReturn(Optional.of(flight));

        adminFlightService.removeFlight(1L);

        assertEquals(FlightStatus.REMOVED, flight.getStatus());
        verify(flightRepository).findById(1L);
        verifyNoMoreInteractions(flightRepository);
    }

    @Test
    void removeFlight_flightNotFound() {
        when(flightRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> adminFlightService.removeFlight(1L));

        verify(flightRepository).findById(1L);
        verifyNoMoreInteractions(flightRepository);
    }
}
