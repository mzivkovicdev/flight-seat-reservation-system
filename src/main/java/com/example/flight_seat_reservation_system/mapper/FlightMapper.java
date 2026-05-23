package com.example.flight_seat_reservation_system.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.example.flight_seat_reservation_system.dto.FlightResponse;
import com.example.flight_seat_reservation_system.entity.Flight;
import com.example.flight_seat_reservation_system.repository.FlightSearchProjection;

@Mapper(config = MapStructConfig.class)
public interface FlightMapper {

    @Mapping(target = "status", expression = "java(flight.getStatus().name())")
    FlightResponse toResponse(Flight flight, long totalSeats, long availableSeats);

    FlightResponse toResponse(FlightSearchProjection projection);
}
