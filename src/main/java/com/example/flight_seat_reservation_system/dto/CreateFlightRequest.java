package com.example.flight_seat_reservation_system.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateFlightRequest(
        @NotBlank @Size(max = 20) String flightNumber,
        @NotBlank @Size(max = 10) @Pattern(regexp = "^[A-Z0-9]{3,10}$") String originAirportCode,
        @NotBlank @Size(max = 100) String originCity,
        @NotBlank @Size(max = 64) String originTimezone,
        @NotBlank @Size(max = 10) @Pattern(regexp = "^[A-Z0-9]{3,10}$") String destinationAirportCode,
        @NotBlank @Size(max = 100) String destinationCity,
        @NotBlank @Size(max = 64) String destinationTimezone,
        @NotNull LocalDateTime departureTimeLocal,
        @NotNull @Min(1) @Max(300) Integer seatCount) {
}
