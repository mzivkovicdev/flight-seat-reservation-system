package com.example.flight_seat_reservation_system.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.example.flight_seat_reservation_system.dto.CreateFlightRequest;
import com.example.flight_seat_reservation_system.dto.FlightResponse;
import com.example.flight_seat_reservation_system.service.AdminFlightService;

@ExtendWith(MockitoExtension.class)
class AdminFlightsControllerTest {

    @Mock
    private AdminFlightService adminFlightService;

    private AdminFlightsController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminFlightsController(adminFlightService);
    }

    @Test
    void createFlight() {
        CreateFlightRequest request = Instancio.create(CreateFlightRequest.class);
        FlightResponse response = Instancio.create(FlightResponse.class);
        when(adminFlightService.createFlight(request)).thenReturn(response);

        var entity = controller.createFlight(request);

        assertEquals(HttpStatus.CREATED, entity.getStatusCode());
        assertEquals(response, entity.getBody());
        verify(adminFlightService).createFlight(request);
        verifyNoMoreInteractions(adminFlightService);
    }

    @Test
    void removeFlight() {
        var entity = controller.removeFlight(1L);

        assertEquals(HttpStatus.NO_CONTENT, entity.getStatusCode());
        verify(adminFlightService).removeFlight(1L);
        verifyNoMoreInteractions(adminFlightService);
    }
}
