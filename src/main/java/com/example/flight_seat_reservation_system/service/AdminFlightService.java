package com.example.flight_seat_reservation_system.service;

import java.time.ZoneId;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.flight_seat_reservation_system.dto.CreateFlightRequest;
import com.example.flight_seat_reservation_system.dto.FlightResponse;
import com.example.flight_seat_reservation_system.entity.Flight;
import com.example.flight_seat_reservation_system.entity.FlightStatus;
import com.example.flight_seat_reservation_system.entity.Seat;
import com.example.flight_seat_reservation_system.exception.NotFoundException;
import com.example.flight_seat_reservation_system.mapper.CreateFlightMapper;
import com.example.flight_seat_reservation_system.mapper.FlightMapper;
import com.example.flight_seat_reservation_system.repository.FlightRepository;
import com.example.flight_seat_reservation_system.service.validation.SeatNumberGenerator;

@Service
public class AdminFlightService {

    private final FlightRepository flightRepository;
    private final CreateFlightMapper createFlightMapper;
    private final FlightMapper flightMapper;
    private final SeatNumberGenerator seatNumberGenerator;

    public AdminFlightService(FlightRepository flightRepository,
                              CreateFlightMapper createFlightMapper,
                              FlightMapper flightMapper,
                              SeatNumberGenerator seatNumberGenerator) {
        this.flightRepository = flightRepository;
        this.createFlightMapper = createFlightMapper;
        this.flightMapper = flightMapper;
        this.seatNumberGenerator = seatNumberGenerator;
    }

    /**
     * Creates a new active flight and generates seats during creation.
     *
     * @param request flight creation payload
     * @return created flight response with calculated seat availability
     */
    @Transactional
    public FlightResponse createFlight(CreateFlightRequest request) {
        Flight flight = createFlightMapper.toEntity(request);
        ZoneId originZone = ZoneId.of(request.originTimezone());

        flight.setDepartureTimeUtc(request.departureTimeLocal().atZone(originZone).toInstant());
        flight.setDepartureDateLocal(request.departureTimeLocal().toLocalDate());
        flight.setStatus(FlightStatus.ACTIVE);

        List<String> seatNumbers = seatNumberGenerator.generate(request.seatCount());
        seatNumbers.stream()
                .map(seatNumber -> {
                    Seat seat = new Seat();
                    seat.setSeatNumber(seatNumber);
                    return seat;
                })
                .forEach(flight::addSeat);

        Flight saved = flightRepository.save(flight);
        return flightMapper.toResponse(saved, saved.getSeats().size(), saved.getSeats().size());
    }

    /**
     * Soft-removes a flight. Existing records remain for history.
     *
     * @param flightId flight identifier
     * @throws NotFoundException if the flight does not exist
     */
    @Transactional
    public void removeFlight(Long flightId) {
        Flight flight = flightRepository.findById(flightId)
                .orElseThrow(() -> new NotFoundException("Flight not found"));
        flight.setStatus(FlightStatus.REMOVED);
    }
}
