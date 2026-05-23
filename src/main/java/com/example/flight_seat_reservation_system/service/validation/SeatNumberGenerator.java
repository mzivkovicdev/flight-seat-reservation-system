package com.example.flight_seat_reservation_system.service.validation;

import java.util.List;
import java.util.stream.IntStream;

import org.springframework.stereotype.Component;

@Component
public class SeatNumberGenerator {

    /**
     * Generates seat numbers like 1A, 1B, ... based on a simple 6-seat row layout.
     *
     * @param seatCount number of seats to generate
     * @return generated seat labels in row-major order
     */
    public List<String> generate(int seatCount) {
        return IntStream.range(0, seatCount)
                .mapToObj(index -> {
                    int row = (index / 6) + 1;
                    char letter = (char) ('A' + (index % 6));
                    return row + String.valueOf(letter);
                })
                .toList();
    }
}
