package com.example.orderservice;

import java.time.Instant;

public class OrderEvent {
    private String orderId;
    private String product;
    private int quantity;
    private String customerId;
    private Instant created;

    public OrderEvent(String orderId, String product, int quantity, String customerId, Instant created) {
        this.orderId = orderId;
        this.product = product;
        this.quantity = quantity;
        this.customerId = customerId;
        this.created = created != null ? created : Instant.now();
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getProduct() { return product; }
    public void setProduct(String product) { this.product = product; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public Instant getCreated() { return created; }
    public void setCreated(Instant created) { this.created = created; }
}
