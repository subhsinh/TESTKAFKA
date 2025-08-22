package com.example.fulfillmentservice.model;

import java.time.Instant;
import java.util.Objects;

/**
 * All fulfillment state transitions and events, persisted and published for event sourcing, saga, and Kafka.
 */
public class FulfillmentEvent {
    private String eventId;           // Unique for dedupe/idempotency (UUID)
    private String orderId;
    private FulfillmentStatus status;
    private String type;              // e.g. "AllocationSucceeded", "AllocationFailed"
    private String payload;           // JSON-serialized snapshot (for audit/event sourcing)
    private Instant timestamp;
    private String correlationId;     // For saga tracking
    private String compensationFor;   // If this event is a compensation/rollback, orderId of the original

    public FulfillmentEvent() {}

    public FulfillmentEvent(String eventId, String orderId, FulfillmentStatus status, String type, String payload, Instant timestamp, String correlationId, String compensationFor) {
        this.eventId = eventId;
        this.orderId = orderId;
        this.status = status;
        this.type = type;
        this.payload = payload;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.correlationId = correlationId;
        this.compensationFor = compensationFor;
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public FulfillmentStatus getStatus() { return status; }
    public void setStatus(FulfillmentStatus status) { this.status = status; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public String getCompensationFor() { return compensationFor; }
    public void setCompensationFor(String compensationFor) { this.compensationFor = compensationFor; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FulfillmentEvent)) return false;
        FulfillmentEvent that = (FulfillmentEvent) o;
        return Objects.equals(eventId, that.eventId) &&
                Objects.equals(orderId, that.orderId) &&
                status == that.status &&
                Objects.equals(type, that.type) &&
                Objects.equals(payload, that.payload) &&
                Objects.equals(timestamp, that.timestamp) &&
                Objects.equals(correlationId, that.correlationId) &&
                Objects.equals(compensationFor, that.compensationFor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId, orderId, status, type, payload, timestamp, correlationId, compensationFor);
    }
}
