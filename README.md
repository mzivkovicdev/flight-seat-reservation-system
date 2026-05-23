# Flight Seat Reservation System

## Overview
This project implements a simplified flight reservation backend for searching flights, holding seats, confirming bookings, and cancellation with history preservation.

Core business behaviors:
- Flights can be searched by date and route.
- Booking creation starts in `HELD` status.
- Booking confirmation moves `HELD -> CONFIRMED`.
- Cancellation moves `HELD/CONFIRMED -> CANCELLED`.
- Held bookings expire and become `EXPIRED`.
- Flights are soft-deleted (`ACTIVE` -> `REMOVED`).

## Tech Stack
- Java 21
- Spring Boot 3.5
- Spring Web
- Spring Data JPA (`JpaRepository`)
- PostgreSQL
- Flyway
- Bean Validation
- MapStruct
- springdoc OpenAPI / Swagger UI
- JUnit 5 + Mockito
- Testcontainers (PostgreSQL)

## Package Structure
`com.example.flight_seat_reservation_system`
- `api`: controller interfaces
- `controller`: thin REST controllers
- `service`: business logic + transactions
- `service.validation`: domain rules and helpers
- `entity`: JPA entities
- `repository`: JPA repositories + query projections
- `mapper`: MapStruct mappers
- `exception`: domain exceptions + global handler
- `config`: Clock, OpenAPI, properties, scheduling
- `scheduler`: held booking expiration job

## How To Run Locally
1. Start PostgreSQL (recommended via Docker):
```bash
docker compose up -d
```
2. Run application:
```bash
mvn spring-boot:run
```
3. Swagger UI:
- `http://localhost:8080/swagger-ui.html`

## How To Run Tests
```bash
mvn clean test
```

Integration tests use Testcontainers with real PostgreSQL (no H2), so Docker must be available.
If Docker is unavailable in the execution environment, the Testcontainers integration test class is skipped (`@Testcontainers(disabledWithoutDocker = true)`).

## Database Migrations
Flyway scripts are in `src/main/resources/db/migration`:
- `V1__create_flights.sql`
- `V2__create_seats.sql`
- `V3__create_bookings.sql`
- `V4__add_indexes.sql`

Important DB constraint:
```sql
CREATE UNIQUE INDEX ux_active_booking_per_seat
ON bookings (flight_id, seat_id)
WHERE status IN ('HELD', 'CONFIRMED');
```

This prevents double booking at database level for active reservations.

## API Endpoints
- `GET /flights`
- `POST /flights/{id}/bookings`
- `POST /bookings/{id}/confirm`
- `DELETE /bookings/{id}`
- `POST /admin/flights`
- `DELETE /admin/flights/{id}`

OpenAPI is generated via springdoc and available in Swagger UI.

## Design Decisions
- Controllers are thin; services hold business logic.
- DTOs are used for all requests/responses; entities are never returned directly.
- `Clock` is injected for testable time-dependent logic.
- Booking cutoff is evaluated in `originTimezone` (not server timezone).
- `HELD` expiration is implemented with both:
  - Scheduled cleanup job (`BookingExpirationScheduler`)
  - Lazy expiration during booking/confirm operations.
- Flight deletion is soft delete (`REMOVED`) to preserve historical data and references.

## Concurrency and Overselling Protection
Two layers are used:
1. Transactional service methods with pessimistic locking on seat selection.
2. PostgreSQL partial unique index for active bookings.

If a race still hits a DB constraint, `DataIntegrityViolationException` is translated to HTTP `409 Conflict`.

## N+1 Avoidance
`GET /flights` uses projection query with joins and aggregated counts (`totalSeats`, `availableSeats`) so flight availability is computed in SQL rather than loading object graphs per flight.

## MapStruct Usage
MapStruct is the default mapper approach:
- `CreateFlightMapper`
- `FlightMapper`
- `BookingMapper`

No manual mapper classes are used.

## OpenAPI Approach
For this take-home scope, controller interfaces (`FlightsApi`, `BookingsApi`, `AdminFlightsApi`) are used to keep an OpenAPI-first-compatible structure without full spec-first code generation complexity.

Trade-off:
- Full generated controllers/models from `openapi.yaml` were intentionally skipped to keep implementation lightweight and focused on business logic/testing.

## Limitations and Future Improvements
- Admin endpoints are intentionally unsecured for assignment scope.
- Search currently returns non-paginated results.
- Flight creation currently supports seat count-based generation (simple 6-seat row pattern), not complex cabin layouts.
- Could add idempotency keys and outbox/eventing for production-grade booking workflows.

## Sample cURL
Create flight:
```bash
curl -X POST http://localhost:8080/admin/flights \
  -H 'Content-Type: application/json' \
  -d '{
    "flightNumber":"FR123",
    "originAirportCode":"DUB",
    "originCity":"Dublin",
    "originTimezone":"Europe/Dublin",
    "destinationAirportCode":"BER",
    "destinationCity":"Berlin",
    "destinationTimezone":"Europe/Berlin",
    "departureTimeLocal":"2026-08-20T14:30:00",
    "seatCount":12
  }'
```

Create booking:
```bash
curl -X POST http://localhost:8080/flights/1/bookings \
  -H 'Content-Type: application/json' \
  -d '{"seatNumber":"1A","passengerName":"Jane Doe","passengerEmail":"jane@example.com"}'
```
