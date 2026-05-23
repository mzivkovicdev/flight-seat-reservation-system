package com.example.flight_seat_reservation_system.service.validation;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class SeatNumberGenerator {

    /**
     * Generates seat numbers like 1A, 1B, ... based on a simple 6-seat row layout.
     */
    public List<String> generate(int seatCount) {
        List<String> seatNumbers = new ArrayList<>(seatCount);
        int row = 1;
        char letter = 'A';

        for (int i = 0; i < seatCount; i++) {
            seatNumbers.add(row + String.valueOf(letter));
            letter++;
            if (letter > 'F') {
                letter = 'A';
                row++;
            }
        }

        return seatNumbers;
    }
}
