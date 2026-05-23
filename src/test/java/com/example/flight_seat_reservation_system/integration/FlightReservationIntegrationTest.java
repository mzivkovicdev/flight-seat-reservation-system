package com.example.flight_seat_reservation_system.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.example.flight_seat_reservation_system.entity.Booking;
import com.example.flight_seat_reservation_system.entity.BookingStatus;
import com.example.flight_seat_reservation_system.entity.Flight;
import com.example.flight_seat_reservation_system.entity.FlightStatus;
import com.example.flight_seat_reservation_system.entity.Seat;
import com.example.flight_seat_reservation_system.repository.BookingRepository;
import com.example.flight_seat_reservation_system.repository.FlightRepository;
import com.example.flight_seat_reservation_system.repository.SeatRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class FlightReservationIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private FlightRepository flightRepository;

    @Autowired
    private SeatRepository seatRepository;

    @BeforeEach
    void clean() {
        bookingRepository.deleteAll();
        seatRepository.deleteAll();
        flightRepository.deleteAll();
    }

    @Test
    void shouldCreateAndSearchFlight() throws Exception {
        Long flightId = createFlight(daysFromNowInOrigin("Europe/Dublin", 2));
        assertThat(flightId).isNotNull();

        MvcResult searchResult = mockMvc.perform(get("/flights")
                        .param("origin", "DUB")
                        .param("destination", "BER"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(searchResult.getResponse().getContentAsString());
        assertThat(root.path("flights").isArray()).isTrue();
        assertThat(root.path("flights").size()).isEqualTo(1);
        assertThat(root.path("flights").get(0).path("totalSeats").asInt()).isEqualTo(12);
        assertThat(root.path("flights").get(0).path("availableSeats").asInt()).isEqualTo(12);
    }

    @Test
    void shouldCreateConfirmAndCancelBooking() throws Exception {
        Long flightId = createFlight(daysFromNowInOrigin("Europe/Dublin", 2));

        MvcResult bookingResult = createBooking(flightId, "1A", "John", "john@example.com", 201);
        Long bookingId = json(bookingResult).path("id").asLong();
        assertThat(json(bookingResult).path("status").asText()).isEqualTo("HELD");

        MvcResult confirmResult = mockMvc.perform(post("/bookings/{id}/confirm", bookingId))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(json(confirmResult).path("status").asText()).isEqualTo("CONFIRMED");

        mockMvc.perform(delete("/bookings/{id}", bookingId))
                .andExpect(status().isNoContent());

        assertThat(bookingRepository.findById(bookingId)).isPresent();
        assertThat(bookingRepository.findById(bookingId).orElseThrow().getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void shouldRejectBookingWithinCutoffWindow() throws Exception {
        Long flightId = createFlight(minutesFromNowInOrigin("Europe/Dublin", 30));

        createBooking(flightId, "1A", "Late User", "late@example.com", 409);
    }

    @Test
    void shouldExpireHoldLazilyAndAllowNewBooking() throws Exception {
        Long flightId = createFlight(daysFromNowInOrigin("Europe/Dublin", 2));

        MvcResult first = createBooking(flightId, "1A", "User One", "u1@example.com", 201);
        Long firstBookingId = json(first).path("id").asLong();

        Booking held = bookingRepository.findById(firstBookingId).orElseThrow();
        held.setHoldExpiresAt(java.time.Instant.now().minusSeconds(5));
        bookingRepository.saveAndFlush(held);

        mockMvc.perform(post("/bookings/{id}/confirm", firstBookingId))
                .andExpect(status().isConflict());

        createBooking(flightId, "1A", "User Two", "u2@example.com", 201);
    }

    @Test
    void shouldBlockRemovedFlightFromNewBookings() throws Exception {
        Long flightId = createFlight(daysFromNowInOrigin("Europe/Dublin", 2));

        mockMvc.perform(delete("/admin/flights/{id}", flightId))
                .andExpect(status().isNoContent());

        createBooking(flightId, "1A", "John", "john@example.com", 409);
    }

    @Test
    void concurrentBookingShouldHaveExactlyOneSuccess() throws Exception {
        Long flightId = createFlight(daysFromNowInOrigin("Europe/Dublin", 2));

        int attempts = 8;
        CountDownLatch ready = new CountDownLatch(attempts);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(attempts);

        List<Future<Integer>> futures = new ArrayList<>();
        for (int i = 0; i < attempts; i++) {
            int idx = i;
            futures.add(pool.submit(() -> {
                ready.countDown();
                start.await();
                MvcResult result = createBooking(flightId, "1A", "U" + idx, "u" + idx + "@example.com", -1);
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

    @Test
    void shouldEnforcePostgresPartialUniqueIndexForActiveBookings() {
        Flight flight = new Flight();
        flight.setFlightNumber("FR200");
        flight.setOriginAirportCode("DUB");
        flight.setOriginCity("Dublin");
        flight.setOriginTimezone("Europe/Dublin");
        flight.setDestinationAirportCode("BER");
        flight.setDestinationCity("Berlin");
        flight.setDestinationTimezone("Europe/Berlin");
        LocalDateTime departureLocal = daysFromNowInOrigin("Europe/Dublin", 3);
        flight.setDepartureTimeLocal(departureLocal);
        flight.setDepartureDateLocal(departureLocal.toLocalDate());
        flight.setDepartureTimeUtc(departureLocal.atZone(ZoneId.of("Europe/Dublin")).toInstant());
        flight.setStatus(FlightStatus.ACTIVE);

        Seat seat = new Seat();
        seat.setSeatNumber("1A");
        flight.addSeat(seat);

        flightRepository.saveAndFlush(flight);

        Booking booking1 = new Booking();
        booking1.setFlight(flight);
        booking1.setSeat(seat);
        booking1.setPassengerName("First");
        booking1.setPassengerEmail("first@example.com");
        booking1.setStatus(BookingStatus.HELD);
        booking1.setHoldExpiresAt(java.time.Instant.now().plusSeconds(120));
        bookingRepository.saveAndFlush(booking1);

        Booking booking2 = new Booking();
        booking2.setFlight(flight);
        booking2.setSeat(seat);
        booking2.setPassengerName("Second");
        booking2.setPassengerEmail("second@example.com");
        booking2.setStatus(BookingStatus.CONFIRMED);

        boolean raised = false;
        try {
            bookingRepository.saveAndFlush(booking2);
        } catch (DataIntegrityViolationException ex) {
            raised = true;
        }

        assertTrue(raised);
    }

    private MvcResult createBooking(Long flightId,
                                    String seatNumber,
                                    String passengerName,
                                    String passengerEmail,
                                    int expectedStatus) throws Exception {
        Map<String, Object> payload = Map.of(
                "seatNumber", seatNumber,
                "passengerName", passengerName,
                "passengerEmail", passengerEmail
        );

        var request = mockMvc.perform(post("/flights/{id}/bookings", flightId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)));

        if (expectedStatus >= 0) {
            request.andExpect(status().is(expectedStatus));
        }

        return request.andReturn();
    }

    private Long createFlight(LocalDateTime departureLocal) throws Exception {
        Map<String, Object> payload = Map.of(
                "flightNumber", "FR100",
                "originAirportCode", "DUB",
                "originCity", "Dublin",
                "originTimezone", "Europe/Dublin",
                "destinationAirportCode", "BER",
                "destinationCity", "Berlin",
                "destinationTimezone", "Europe/Berlin",
                "departureTimeLocal", departureLocal,
                "seatCount", 12
        );

        MvcResult result = mockMvc.perform(post("/admin/flights")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn();

        return json(result).path("id").asLong();
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private LocalDateTime daysFromNowInOrigin(String timezone, long days) {
        return ZonedDateTime.now(ZoneId.of(timezone)).plusDays(days).withSecond(0).withNano(0).toLocalDateTime();
    }

    private LocalDateTime minutesFromNowInOrigin(String timezone, long minutes) {
        return ZonedDateTime.now(ZoneId.of(timezone)).plusMinutes(minutes).withSecond(0).withNano(0).toLocalDateTime();
    }
}
