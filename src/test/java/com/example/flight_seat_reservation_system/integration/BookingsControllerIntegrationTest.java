package com.example.flight_seat_reservation_system.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import com.example.flight_seat_reservation_system.entity.Booking;
import com.example.flight_seat_reservation_system.entity.BookingStatus;

class BookingsControllerIntegrationTest extends AbstractIntegrationTest {

    @Test
    void confirmBooking() throws Exception {
        Long flightId = createFlight(createFlightRequest(daysFromNowInOrigin("Europe/Dublin", 2)));

        MvcResult bookingResult = createBooking(flightId, createBookingRequest("1A"), 201);
        Long bookingId = json(bookingResult).path("id").asLong();
        assertThat(json(bookingResult).path("status").asText()).isEqualTo("HELD");

        MvcResult confirmResult = mockMvc.perform(post("/bookings/{id}/confirm", bookingId)).andReturn();
        assertThat(confirmResult.getResponse().getStatus()).isEqualTo(200);
        assertThat(json(confirmResult).path("status").asText()).isEqualTo("CONFIRMED");

        MvcResult cancelResult = mockMvc.perform(delete("/bookings/{id}", bookingId)).andReturn();
        assertThat(cancelResult.getResponse().getStatus()).isEqualTo(204);

        assertThat(bookingRepository.findById(bookingId)).isPresent();
        assertThat(bookingRepository.findById(bookingId).orElseThrow().getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void confirmBooking_holdExpired() throws Exception {
        Long flightId = createFlight(createFlightRequest(daysFromNowInOrigin("Europe/Dublin", 2)));

        MvcResult first = createBooking(flightId, createBookingRequest("1A"), 201);
        Long firstBookingId = json(first).path("id").asLong();

        Booking held = bookingRepository.findById(firstBookingId).orElseThrow();
        held.setHoldExpiresAt(java.time.Instant.now().minusSeconds(5));
        bookingRepository.saveAndFlush(held);

        MvcResult confirmExpired = mockMvc.perform(post("/bookings/{id}/confirm", firstBookingId)).andReturn();
        assertThat(confirmExpired.getResponse().getStatus()).isEqualTo(409);

        createBooking(flightId, createBookingRequest("1A"), 201);
    }
}
