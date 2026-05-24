package com.example.flight_seat_reservation_system.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.flight_seat_reservation_system.dto.CreateFlightRequest;
import com.example.flight_seat_reservation_system.dto.FlightResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Validated
@RequestMapping
@Tag(name = "Admin Flights")
public interface AdminFlightsApi {

    @PostMapping("/admin/flights")
    @Operation(summary = "Create a new flight")
    ResponseEntity<FlightResponse> createFlight(@Valid @RequestBody CreateFlightRequest request);

    @DeleteMapping("/admin/flights/{id}")
    @Operation(summary = "Soft-delete a flight")
    ResponseEntity<Void> removeFlight(@PathVariable("id") @Positive Long flightId);
}
