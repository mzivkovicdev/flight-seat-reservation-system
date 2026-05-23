package com.example.flight_seat_reservation_system.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.flight_seat_reservation_system.service.BookingService;

@Component
public class BookingExpirationScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(BookingExpirationScheduler.class);

    private final BookingService bookingService;

    public BookingExpirationScheduler(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Scheduled(fixedDelayString = "${booking.expiration-cleanup-delay}")
    public void cleanupExpiredHolds() {
        int expired = bookingService.expireHeldBookings();
        if (expired > 0) {
            LOGGER.info("Expired {} held bookings", expired);
        }
    }
}
