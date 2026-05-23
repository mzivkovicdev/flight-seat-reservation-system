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

import com.example.flight_seat_reservation_system.dto.BookingResponse;
import com.example.flight_seat_reservation_system.service.BookingService;

@ExtendWith(MockitoExtension.class)
class BookingsControllerTest {

    @Mock
    private BookingService bookingService;

    private BookingsController controller;

    @BeforeEach
    void setUp() {
        controller = new BookingsController(bookingService);
    }

    @Test
    void confirmBooking() {
        BookingResponse response = Instancio.create(BookingResponse.class);
        when(bookingService.confirmBooking(1L)).thenReturn(response);

        var entity = controller.confirmBooking(1L);

        assertEquals(HttpStatus.OK, entity.getStatusCode());
        assertEquals(response, entity.getBody());
        verify(bookingService).confirmBooking(1L);
        verifyNoMoreInteractions(bookingService);
    }

    @Test
    void cancelBooking() {
        var entity = controller.cancelBooking(1L);

        assertEquals(HttpStatus.NO_CONTENT, entity.getStatusCode());
        verify(bookingService).cancelBooking(1L);
        verifyNoMoreInteractions(bookingService);
    }
}
