package com.example.flight_seat_reservation_system.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "booking")
public class BookingProperties {

    private Duration holdDuration = Duration.ofMinutes(10);
    private Duration cutoffDuration = Duration.ofMinutes(45);
    private Duration expirationCleanupDelay = Duration.ofMinutes(1);

    public Duration getHoldDuration() {
        return holdDuration;
    }

    public void setHoldDuration(Duration holdDuration) {
        this.holdDuration = holdDuration;
    }

    public Duration getCutoffDuration() {
        return cutoffDuration;
    }

    public void setCutoffDuration(Duration cutoffDuration) {
        this.cutoffDuration = cutoffDuration;
    }

    public Duration getExpirationCleanupDelay() {
        return expirationCleanupDelay;
    }

    public void setExpirationCleanupDelay(Duration expirationCleanupDelay) {
        this.expirationCleanupDelay = expirationCleanupDelay;
    }
}
