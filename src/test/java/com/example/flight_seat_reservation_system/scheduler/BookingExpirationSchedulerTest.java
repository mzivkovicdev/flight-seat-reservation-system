package com.example.flight_seat_reservation_system.scheduler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.flight_seat_reservation_system.service.BookingService;

@ExtendWith(MockitoExtension.class)
class BookingExpirationSchedulerTest {

    @Mock
    private BookingService bookingService;

    private BookingExpirationScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new BookingExpirationScheduler(bookingService);
    }

    @Test
    void cleanupExpiredHolds() {
        when(bookingService.expireHeldBookings()).thenReturn(2);

        assertDoesNotThrow(() -> scheduler.cleanupExpiredHolds());

        verify(bookingService).expireHeldBookings();
        verifyNoMoreInteractions(bookingService);
    }

    @Test
    void cleanupExpiredHolds_noExpiredBookings() {
        when(bookingService.expireHeldBookings()).thenReturn(0);

        assertDoesNotThrow(() -> scheduler.cleanupExpiredHolds());

        verify(bookingService).expireHeldBookings();
        verifyNoMoreInteractions(bookingService);
    }
}
