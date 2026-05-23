package com.example.flight_seat_reservation_system.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.instancio.Select.field;
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
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.instancio.Instancio;
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

import com.example.flight_seat_reservation_system.dto.CreateBookingRequest;
import com.example.flight_seat_reservation_system.dto.CreateFlightRequest;
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
        CreateFlightRequest flightRequest = createFlightRequest(daysFromNowInOrigin("Europe/Dublin", 2));
        Long flightId = createFlight(flightRequest);
        assertThat(flightId).isNotNull();

        MvcResult searchResult = mockMvc.perform(get("/flights")
                        .param("origin", flightRequest.originAirportCode())
                        .param("destination", flightRequest.destinationAirportCode()))
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
        Long flightId = createFlight(createFlightRequest(daysFromNowInOrigin("Europe/Dublin", 2)));

        MvcResult bookingResult = createBooking(flightId, createBookingRequest("1A"), 201);
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
        Long flightId = createFlight(createFlightRequest(minutesFromNowInOrigin("Europe/Dublin", 30)));

        createBooking(flightId, createBookingRequest("1A"), 409);
    }

    @Test
    void shouldExpireHoldLazilyAndAllowNewBooking() throws Exception {
        Long flightId = createFlight(createFlightRequest(daysFromNowInOrigin("Europe/Dublin", 2)));

        MvcResult first = createBooking(flightId, createBookingRequest("1A"), 201);
        Long firstBookingId = json(first).path("id").asLong();

        Booking held = bookingRepository.findById(firstBookingId).orElseThrow();
        held.setHoldExpiresAt(java.time.Instant.now().minusSeconds(5));
        bookingRepository.saveAndFlush(held);

        mockMvc.perform(post("/bookings/{id}/confirm", firstBookingId))
                .andExpect(status().isConflict());

        createBooking(flightId, createBookingRequest("1A"), 201);
    }

    @Test
    void shouldBlockRemovedFlightFromNewBookings() throws Exception {
        Long flightId = createFlight(createFlightRequest(daysFromNowInOrigin("Europe/Dublin", 2)));

        mockMvc.perform(delete("/admin/flights/{id}", flightId))
                .andExpect(status().isNoContent());

        createBooking(flightId, createBookingRequest("1A"), 409);
    }

    @Test
    void concurrentBookingShouldHaveExactlyOneSuccess() throws Exception {
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

    @Test
    void shouldEnforcePostgresPartialUniqueIndexForActiveBookings() {
        Flight flight = Instancio.of(Flight.class)
                .set(field(Flight::getFlightNumber), "FR200")
                .set(field(Flight::getOriginAirportCode), "DUB")
                .set(field(Flight::getOriginCity), "Dublin")
                .set(field(Flight::getOriginTimezone), "Europe/Dublin")
                .set(field(Flight::getDestinationAirportCode), "BER")
                .set(field(Flight::getDestinationCity), "Berlin")
                .set(field(Flight::getDestinationTimezone), "Europe/Berlin")
                .set(field(Flight::getStatus), FlightStatus.ACTIVE)
                .create();
        LocalDateTime departureLocal = daysFromNowInOrigin("Europe/Dublin", 3);
        flight.setDepartureTimeLocal(departureLocal);
        flight.setDepartureDateLocal(departureLocal.toLocalDate());
        flight.setDepartureTimeUtc(departureLocal.atZone(ZoneId.of("Europe/Dublin")).toInstant());

        Seat seat = Instancio.of(Seat.class)
                .set(field(Seat::getSeatNumber), "1A")
                .create();
        flight.addSeat(seat);

        flightRepository.saveAndFlush(flight);

        Booking booking1 = Instancio.of(Booking.class)
                .set(field(Booking::getFlight), flight)
                .set(field(Booking::getSeat), seat)
                .set(field(Booking::getPassengerName), "First")
                .set(field(Booking::getPassengerEmail), "first@example.com")
                .set(field(Booking::getStatus), BookingStatus.HELD)
                .set(field(Booking::getHoldExpiresAt), java.time.Instant.now().plusSeconds(120))
                .create();
        bookingRepository.saveAndFlush(booking1);

        Booking booking2 = Instancio.of(Booking.class)
                .set(field(Booking::getFlight), flight)
                .set(field(Booking::getSeat), seat)
                .set(field(Booking::getPassengerName), "Second")
                .set(field(Booking::getPassengerEmail), "second@example.com")
                .set(field(Booking::getStatus), BookingStatus.CONFIRMED)
                .create();

        boolean raised = false;
        try {
            bookingRepository.saveAndFlush(booking2);
        } catch (DataIntegrityViolationException ex) {
            raised = true;
        }

        assertTrue(raised);
    }

    private MvcResult createBooking(Long flightId,
                                    CreateBookingRequest payload,
                                    int expectedStatus) throws Exception {
        var request = mockMvc.perform(post("/flights/{id}/bookings", flightId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)));

        if (expectedStatus >= 0) {
            request.andExpect(status().is(expectedStatus));
        }

        return request.andReturn();
    }

    private Long createFlight(CreateFlightRequest payload) throws Exception {
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

    private CreateFlightRequest createFlightRequest(LocalDateTime departureLocal) {
        return Instancio.of(CreateFlightRequest.class)
                .set(field(CreateFlightRequest::flightNumber), "FL-" + Instancio.create(Integer.class))
                .set(field(CreateFlightRequest::originAirportCode), "DUB")
                .set(field(CreateFlightRequest::originCity), "City-" + Instancio.create(Integer.class))
                .set(field(CreateFlightRequest::originTimezone), "Europe/Dublin")
                .set(field(CreateFlightRequest::destinationAirportCode), "BER")
                .set(field(CreateFlightRequest::destinationCity), "City-" + Instancio.create(Integer.class))
                .set(field(CreateFlightRequest::destinationTimezone), "Europe/Berlin")
                .set(field(CreateFlightRequest::departureTimeLocal), departureLocal)
                .set(field(CreateFlightRequest::seatCount), 12)
                .create();
    }

    private CreateBookingRequest createBookingRequest(String seatNumber) {
        return Instancio.of(CreateBookingRequest.class)
                .set(field(CreateBookingRequest::seatNumber), seatNumber)
                .supply(field(CreateBookingRequest::passengerName), () -> "Passenger-" + UUID.randomUUID())
                .supply(field(CreateBookingRequest::passengerEmail), () -> "user-" + UUID.randomUUID() + "@example.com")
                .create();
    }
}
