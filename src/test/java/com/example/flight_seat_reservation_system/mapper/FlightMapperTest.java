package com.example.flight_seat_reservation_system.mapper;

import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.time.LocalDateTime;

import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import com.example.flight_seat_reservation_system.entity.Flight;
import com.example.flight_seat_reservation_system.entity.FlightStatus;
import com.example.flight_seat_reservation_system.repository.FlightSearchProjection;

class FlightMapperTest {

    @Test
    void toResponse() {
        FlightMapper mapper = spy(Mappers.getMapper(FlightMapper.class));

        Flight flight = Instancio.of(Flight.class)
                .set(field(Flight::getStatus), FlightStatus.ACTIVE)
                .set(field(Flight::getFlightNumber), "FL-" + Instancio.create(Integer.class))
                .create();

        var response = mapper.toResponse(flight, 12L, 10L);

        assertNotNull(response);
        assertEquals("ACTIVE", response.status());
        assertEquals(12L, response.totalSeats());
        assertEquals(10L, response.availableSeats());
        verify(mapper).toResponse(flight, 12L, 10L);
    }

    @Test
    void toResponse_projection() {
        FlightMapper mapper = spy(Mappers.getMapper(FlightMapper.class));

        FlightSearchProjection projection = new FlightSearchProjection() {
            @Override
            public Long getId() {
                return 1L;
            }

            @Override
            public String getFlightNumber() {
                return "FL-101";
            }

            @Override
            public String getOriginAirportCode() {
                return "DUB";
            }

            @Override
            public String getOriginCity() {
                return "Dublin";
            }

            @Override
            public String getOriginTimezone() {
                return "Europe/Dublin";
            }

            @Override
            public String getDestinationAirportCode() {
                return "BER";
            }

            @Override
            public String getDestinationCity() {
                return "Berlin";
            }

            @Override
            public String getDestinationTimezone() {
                return "Europe/Berlin";
            }

            @Override
            public LocalDateTime getDepartureTimeLocal() {
                return LocalDateTime.parse("2026-08-20T14:30:00");
            }

            @Override
            public Instant getDepartureTimeUtc() {
                return Instant.parse("2026-08-20T13:30:00Z");
            }

            @Override
            public String getStatus() {
                return "ACTIVE";
            }

            @Override
            public long getTotalSeats() {
                return 12;
            }

            @Override
            public long getAvailableSeats() {
                return 9;
            }
        };

        var response = mapper.toResponse(projection);

        assertNotNull(response);
        assertEquals("FL-101", response.flightNumber());
        assertEquals(12L, response.totalSeats());
        assertEquals(9L, response.availableSeats());
        verify(mapper).toResponse(projection);
    }
}
