package com.example.flight_seat_reservation_system.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

class AdminFlightsControllerIntegrationTest extends AbstractIntegrationTest {

    @Test
    void createFlight() throws Exception {
        Long flightId = createFlight(createFlightRequest(daysFromNowInOrigin("Europe/Dublin", 2)));
        org.assertj.core.api.Assertions.assertThat(flightId).isNotNull();
    }

    @Test
    void removeFlight_flightRemovedBlocksNewBookings() throws Exception {
        Long flightId = createFlight(createFlightRequest(daysFromNowInOrigin("Europe/Dublin", 2)));

        MvcResult deleteResult = mockMvc.perform(delete("/admin/flights/{id}", flightId)).andReturn();
        org.assertj.core.api.Assertions.assertThat(deleteResult.getResponse().getStatus()).isEqualTo(204);

        createBooking(flightId, createBookingRequest("1A"), 409);
    }
}
