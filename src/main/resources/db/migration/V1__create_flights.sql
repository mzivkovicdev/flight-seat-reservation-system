CREATE TABLE flights (
    id BIGSERIAL PRIMARY KEY,
    flight_number VARCHAR(20) NOT NULL,
    origin_airport_code VARCHAR(10) NOT NULL,
    origin_city VARCHAR(100) NOT NULL,
    origin_timezone VARCHAR(64) NOT NULL,
    destination_airport_code VARCHAR(10) NOT NULL,
    destination_city VARCHAR(100) NOT NULL,
    destination_timezone VARCHAR(64) NOT NULL,
    departure_time_utc TIMESTAMPTZ NOT NULL,
    departure_time_local TIMESTAMP NOT NULL,
    departure_date_local DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_flights_status CHECK (status IN ('ACTIVE', 'REMOVED'))
);
