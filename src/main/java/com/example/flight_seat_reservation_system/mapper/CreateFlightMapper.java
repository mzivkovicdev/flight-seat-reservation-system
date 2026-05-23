package com.example.flight_seat_reservation_system.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.example.flight_seat_reservation_system.dto.CreateFlightRequest;
import com.example.flight_seat_reservation_system.entity.Flight;

@Mapper(config = MapStructConfig.class)
public interface CreateFlightMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "departureTimeUtc", ignore = true)
    @Mapping(target = "departureDateLocal", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "seats", ignore = true)
    Flight toEntity(CreateFlightRequest request);
}
