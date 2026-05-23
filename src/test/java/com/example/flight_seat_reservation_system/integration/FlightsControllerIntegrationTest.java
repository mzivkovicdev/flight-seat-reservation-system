package com.example.flight_seat_reservation_system.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;

class FlightsControllerIntegrationTest extends AbstractIntegrationTest {

    @Test
    void getFlights() throws Exception {
        var flightRequest = createFlightRequest(daysFromNowInOrigin("Europe/Dublin", 2));
        Long flightId = createFlight(flightRequest);
        assertThat(flightId).isNotNull();

        MvcResult searchResult = mockMvc.perform(get("/flights")
                        .param("origin", flightRequest.originAirportCode())
                        .param("destination", flightRequest.destinationAirportCode()))
                .andReturn();

        assertEquals(200, searchResult.getResponse().getStatus());
        JsonNode root = objectMapper.readTree(searchResult.getResponse().getContentAsString());
        assertThat(root.path("flights").isArray()).isTrue();
        assertThat(root.path("flights").size()).isEqualTo(1);
        assertThat(root.path("flights").get(0).path("totalSeats").asInt()).isEqualTo(12);
        assertThat(root.path("flights").get(0).path("availableSeats").asInt()).isEqualTo(12);
    }

    @Test
    void createBooking_bookingWindowClosed() throws Exception {
        Long flightId = createFlight(createFlightRequest(minutesFromNowInOrigin("Europe/Dublin", 30)));
        createBooking(flightId, createBookingRequest("1A"), 409);
    }

    @Test
    void createBooking_concurrentRequestsOnlyOneSuccess() throws Exception {
        Long flightId = createFlight(createFlightRequest(daysFromNowInOrigin("Europe/Dublin", 2)));

        int attempts = 8;
        CountDownLatch ready = new CountDownLatch(attempts);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(attempts);

        List<Future<Integer>> futures = new ArrayList<>();
        for (int i = 0; i < attempts; i++) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                start.await();
                MvcResult result = createBooking(flightId, createBookingRequest("1A"), -1);
                return result.getResponse().getStatus();
            }));
        }

        ready.await();
        start.countDown();

        int success = 0;
        int conflict = 0;

        for (Future<Integer> future : futures) {
            int status = future.get();
            if (status == 201) {
                success++;
            }
            if (status == 409) {
                conflict++;
            }
        }

        pool.shutdown();
        assertEquals(1, success);
        assertEquals(attempts - 1, conflict);
    }
}
