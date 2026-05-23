CREATE INDEX idx_flights_departure_time_utc ON flights (departure_time_utc);
CREATE INDEX idx_flights_route_date ON flights (origin_airport_code, destination_airport_code, departure_date_local);
CREATE INDEX idx_flights_cities_date ON flights (origin_city, destination_city, departure_date_local);

CREATE INDEX idx_bookings_flight_seat_status ON bookings (flight_id, seat_id, status);
CREATE INDEX idx_bookings_hold_expiry ON bookings (status, hold_expires_at);

CREATE UNIQUE INDEX ux_active_booking_per_seat
ON bookings (flight_id, seat_id)
WHERE status IN ('HELD', 'CONFIRMED');
