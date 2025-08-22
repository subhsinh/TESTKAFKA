package com.example.fulfillmentservice.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Event streamed from orders topic (placed by order-service)
 */
public class OrderEvent {
    private String orderId;
    private String productId;
    private int quantity;
    private String customerId;
    private Instant created;
    // Optionally: payment, metadata

    public OrderEvent() {}

    public OrderEvent(String orderId, String productId, int quantity, String customerId, Instant created) {
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.customerId = customerId;
        this.created = created != null ? created : Instant.now();
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public Instant getCreated() { return created; }
    public void setCreated(Instant created) { this.created = created; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrderEvent)) return false;
        OrderEvent that = (OrderEvent) o;
        return quantity == that.quantity &&
                Objects.equals(orderId, that.orderId) &&
                Objects.equals(productId, that.productId) &&
                Objects.equals(customerId, that.customerId) &&
                Objects.equals(created, that.created);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId, productId, quantity, customerId, created);
    }
}
