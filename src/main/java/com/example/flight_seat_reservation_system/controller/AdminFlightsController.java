package com.example.flight_seat_reservation_system.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.example.flight_seat_reservation_system.api.AdminFlightsApi;
import com.example.flight_seat_reservation_system.dto.CreateFlightRequest;
import com.example.flight_seat_reservation_system.dto.FlightResponse;
import com.example.flight_seat_reservation_system.service.AdminFlightService;

@RestController
public class AdminFlightsController implements AdminFlightsApi {

    private final AdminFlightService adminFlightService;

    public AdminFlightsController(AdminFlightService adminFlightService) {
        this.adminFlightService = adminFlightService;
    }

    @Override
    public ResponseEntity<FlightResponse> createFlight(CreateFlightRequest request) {
        FlightResponse response = adminFlightService.createFlight(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<Void> removeFlight(Long flightId) {
        adminFlightService.removeFlight(flightId);
        return ResponseEntity.noContent().build();
    }
}
