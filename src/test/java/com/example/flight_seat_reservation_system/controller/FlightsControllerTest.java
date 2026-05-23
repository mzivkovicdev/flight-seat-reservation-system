package com.example.flight_seat_reservation_system.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.example.flight_seat_reservation_system.dto.BookingResponse;
import com.example.flight_seat_reservation_system.dto.CreateBookingRequest;
import com.example.flight_seat_reservation_system.dto.FlightSearchResponse;
import com.example.flight_seat_reservation_system.service.BookingService;
import com.example.flight_seat_reservation_system.service.FlightQueryService;

@ExtendWith(MockitoExtension.class)
class FlightsControllerTest {

    @Mock
    private FlightQueryService flightQueryService;
    @Mock
    private BookingService bookingService;

    private FlightsController controller;

    @BeforeEach
    void setUp() {
        controller = new FlightsController(flightQueryService, bookingService);
    }

    @Test
    void getFlights() {
        FlightSearchResponse response = Instancio.create(FlightSearchResponse.class);
        LocalDate date = LocalDate.parse("2026-08-20");

        when(flightQueryService.searchFlights(date, "DUB", "BER")).thenReturn(response);

        var entity = controller.getFlights(date, "DUB", "BER");

        assertEquals(HttpStatus.OK, entity.getStatusCode());
        assertEquals(response, entity.getBody());
        verify(flightQueryService).searchFlights(date, "DUB", "BER");
        verifyNoMoreInteractions(flightQueryService);
    }

    @Test
    void createBooking() {
        CreateBookingRequest request = Instancio.create(CreateBookingRequest.class);
        BookingResponse response = Instancio.create(BookingResponse.class);

        when(bookingService.createBooking(1L, request)).thenReturn(response);

        var entity = controller.createBooking(1L, request);

        assertEquals(HttpStatus.CREATED, entity.getStatusCode());
        assertEquals(response, entity.getBody());
        verify(bookingService).createBooking(1L, request);
        verifyNoMoreInteractions(bookingService);
    }
}
