package com.example.flight_seat_reservation_system.mapper;

import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;

import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import com.example.flight_seat_reservation_system.dto.CreateFlightRequest;

class CreateFlightMapperTest {

    @Test
    void toEntity() {
        CreateFlightMapper mapper = spy(Mappers.getMapper(CreateFlightMapper.class));

        CreateFlightRequest request = Instancio.of(CreateFlightRequest.class)
                .set(field(CreateFlightRequest::flightNumber), "FL-" + Instancio.create(Integer.class))
                .set(field(CreateFlightRequest::originAirportCode), "DUB")
                .set(field(CreateFlightRequest::originCity), "Dublin")
                .set(field(CreateFlightRequest::originTimezone), "Europe/Dublin")
                .set(field(CreateFlightRequest::destinationAirportCode), "BER")
                .set(field(CreateFlightRequest::destinationCity), "Berlin")
                .set(field(CreateFlightRequest::destinationTimezone), "Europe/Berlin")
                .set(field(CreateFlightRequest::departureTimeLocal), LocalDateTime.parse("2026-08-20T14:30:00"))
                .set(field(CreateFlightRequest::seatCount), 12)
                .create();

        var entity = mapper.toEntity(request);

        assertEquals(request.flightNumber(), entity.getFlightNumber());
        assertEquals(request.originAirportCode(), entity.getOriginAirportCode());
        assertEquals(request.destinationAirportCode(), entity.getDestinationAirportCode());
        assertNull(entity.getStatus());
        verify(mapper).toEntity(request);
    }
}
