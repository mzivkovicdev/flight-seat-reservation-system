package com.example.flight_seat_reservation_system.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateBookingRequest(
        @NotBlank @Pattern(regexp = "^[0-9]{1,2}[A-Z]$") String seatNumber,
        @NotBlank @Size(max = 150) String passengerName,
        @NotBlank @Email @Size(max = 255) String passengerEmail) {
}
