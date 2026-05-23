package com.example.flight_seat_reservation_system.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "flights")
public class Flight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String flightNumber;

    @Column(nullable = false, length = 10)
    private String originAirportCode;

    @Column(nullable = false, length = 100)
    private String originCity;

    @Column(nullable = false, length = 64)
    private String originTimezone;

    @Column(nullable = false, length = 10)
    private String destinationAirportCode;

    @Column(nullable = false, length = 100)
    private String destinationCity;

    @Column(nullable = false, length = 64)
    private String destinationTimezone;

    @Column(nullable = false)
    private Instant departureTimeUtc;

    @Column(nullable = false)
    private LocalDateTime departureTimeLocal;

    @Column(nullable = false)
    private LocalDate departureDateLocal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FlightStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    @OneToMany(mappedBy = "flight", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Seat> seats = new ArrayList<>();

    public Flight() {
    }

    public Flight(String flightNumber, String originAirportCode, String originCity, String originTimezone,
                  String destinationAirportCode, String destinationCity, String destinationTimezone,
                  Instant departureTimeUtc, LocalDateTime departureTimeLocal, LocalDate departureDateLocal,
                  FlightStatus status, List<Seat> seats) {
        this.flightNumber = flightNumber;
        this.originAirportCode = originAirportCode;
        this.originCity = originCity;
        this.originTimezone = originTimezone;
        this.destinationAirportCode = destinationAirportCode;
        this.destinationCity = destinationCity;
        this.destinationTimezone = destinationTimezone;
        this.departureTimeUtc = departureTimeUtc;
        this.departureTimeLocal = departureTimeLocal;
        this.departureDateLocal = departureDateLocal;
        this.status = status;
        this.seats = seats != null ? seats : new ArrayList<>();
    }

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public void addSeat(Seat seat) {
        seats.add(seat);
        seat.setFlight(this);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public void setFlightNumber(String flightNumber) {
        this.flightNumber = flightNumber;
    }

    public String getOriginAirportCode() {
        return originAirportCode;
    }

    public void setOriginAirportCode(String originAirportCode) {
        this.originAirportCode = originAirportCode;
    }

    public String getOriginCity() {
        return originCity;
    }

    public void setOriginCity(String originCity) {
        this.originCity = originCity;
    }

    public String getOriginTimezone() {
        return originTimezone;
    }

    public void setOriginTimezone(String originTimezone) {
        this.originTimezone = originTimezone;
    }

    public String getDestinationAirportCode() {
        return destinationAirportCode;
    }

    public void setDestinationAirportCode(String destinationAirportCode) {
        this.destinationAirportCode = destinationAirportCode;
    }

    public String getDestinationCity() {
        return destinationCity;
    }

    public void setDestinationCity(String destinationCity) {
        this.destinationCity = destinationCity;
    }

    public String getDestinationTimezone() {
        return destinationTimezone;
    }

    public void setDestinationTimezone(String destinationTimezone) {
        this.destinationTimezone = destinationTimezone;
    }

    public Instant getDepartureTimeUtc() {
        return departureTimeUtc;
    }

    public void setDepartureTimeUtc(Instant departureTimeUtc) {
        this.departureTimeUtc = departureTimeUtc;
    }

    public LocalDateTime getDepartureTimeLocal() {
        return departureTimeLocal;
    }

    public void setDepartureTimeLocal(LocalDateTime departureTimeLocal) {
        this.departureTimeLocal = departureTimeLocal;
    }

    public LocalDate getDepartureDateLocal() {
        return departureDateLocal;
    }

    public void setDepartureDateLocal(LocalDate departureDateLocal) {
        this.departureDateLocal = departureDateLocal;
    }

    public FlightStatus getStatus() {
        return status;
    }

    public void setStatus(FlightStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public List<Seat> getSeats() {
        return seats;
    }

    public void setSeats(List<Seat> seats) {
        this.seats = seats;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Flight)) {
            return false;
        }
        Flight flight = (Flight) o;
        return id != null && Objects.equals(id, flight.id);
    }

    @Override
    public final int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Flight{" +
                "id=" + id +
                ", flightNumber='" + flightNumber + '\'' +
                ", originAirportCode='" + originAirportCode + '\'' +
                ", originCity='" + originCity + '\'' +
                ", originTimezone='" + originTimezone + '\'' +
                ", destinationAirportCode='" + destinationAirportCode + '\'' +
                ", destinationCity='" + destinationCity + '\'' +
                ", destinationTimezone='" + destinationTimezone + '\'' +
                ", departureTimeUtc=" + departureTimeUtc +
                ", departureTimeLocal=" + departureTimeLocal +
                ", departureDateLocal=" + departureDateLocal +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", version=" + version +
                '}';
    }
}
