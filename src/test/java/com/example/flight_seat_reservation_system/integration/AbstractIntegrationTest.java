package com.example.flight_seat_reservation_system.integration;

import static org.instancio.Select.field;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.example.flight_seat_reservation_system.dto.CreateBookingRequest;
import com.example.flight_seat_reservation_system.dto.CreateFlightRequest;
import com.example.flight_seat_reservation_system.repository.BookingRepository;
import com.example.flight_seat_reservation_system.repository.FlightRepository;
import com.example.flight_seat_reservation_system.repository.SeatRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    protected MockMvc mockMvc;

    protected final ObjectMapper objectMapper = JsonMapper.builder()
            .findAndAddModules()
            .build();

    @Autowired
    protected BookingRepository bookingRepository;

    @Autowired
    protected FlightRepository flightRepository;

    @Autowired
    protected SeatRepository seatRepository;

    @BeforeEach
    void clean() {
        bookingRepository.deleteAll();
        seatRepository.deleteAll();
        flightRepository.deleteAll();
    }

    protected MvcResult createBooking(Long flightId,
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

    protected Long createFlight(CreateFlightRequest payload) throws Exception {
        MvcResult result = mockMvc.perform(post("/admin/flights")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn();

        return json(result).path("id").asLong();
    }

    protected JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    protected LocalDateTime daysFromNowInOrigin(String timezone, long days) {
        return ZonedDateTime.now(ZoneId.of(timezone)).plusDays(days).withSecond(0).withNano(0).toLocalDateTime();
    }

    protected LocalDateTime minutesFromNowInOrigin(String timezone, long minutes) {
        return ZonedDateTime.now(ZoneId.of(timezone)).plusMinutes(minutes).withSecond(0).withNano(0).toLocalDateTime();
    }

    protected CreateFlightRequest createFlightRequest(LocalDateTime departureLocal) {
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

    protected CreateBookingRequest createBookingRequest(String seatNumber) {
        return Instancio.of(CreateBookingRequest.class)
                .set(field(CreateBookingRequest::seatNumber), seatNumber)
                .supply(field(CreateBookingRequest::passengerName), () -> "Passenger-" + UUID.randomUUID())
                .supply(field(CreateBookingRequest::passengerEmail), () -> "user-" + UUID.randomUUID() + "@example.com")
                .create();
    }
}
