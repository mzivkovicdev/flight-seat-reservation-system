package com.example.flight_seat_reservation_system.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.example.flight_seat_reservation_system.dto.BookingResponse;
import com.example.flight_seat_reservation_system.entity.Booking;

@Mapper(config = MapStructConfig.class)
public interface BookingMapper {

    @Mapping(target = "flightId", source = "flight.id")
    @Mapping(target = "flightNumber", source = "flight.flightNumber")
    @Mapping(target = "seatNumber", source = "seat.seatNumber")
    @Mapping(target = "status", expression = "java(booking.getStatus().name())")
    BookingResponse toResponse(Booking booking);
}
