package com.example.fulfillmentservice.model;

/**
 * Status values for order fulfillment saga.
 */
public enum FulfillmentStatus {
    NEW,       // Order received, not started
    ALLOCATING, // Inventory requested/allocating
    ALLOCATED, // Inventory allocated
    PAYMENT_PENDING,
    PAYMENT_RECEIVED,
    SHIPPING,
    SHIPPED,   // Out for delivery/shipped
    DELIVERED,
    CANCELLED,
    FAILED,
    COMPENSATED // Rollback complete
}
