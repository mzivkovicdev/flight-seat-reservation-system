package com.example.flight_seat_reservation_system.service.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.Test;

class SeatNumberGeneratorTest {

    @Test
    void generate() {
        SeatNumberGenerator generator = spy(new SeatNumberGenerator());

        List<String> seats = generator.generate(8);

        assertEquals(8, seats.size());
        assertEquals("1A", seats.get(0));
        assertEquals("1F", seats.get(5));
        assertEquals("2A", seats.get(6));
        assertTrue(seats.contains("2B"));
        verify(generator).generate(8);
    }
}
