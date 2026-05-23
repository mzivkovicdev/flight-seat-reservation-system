package com.example.flight_seat_reservation_system.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI flightReservationOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Flight Seat Reservation API")
                .version("1.0.0")
                .description("API for searching flights and managing seat reservations"));
    }
}
