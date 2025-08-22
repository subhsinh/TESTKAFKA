package com.example.orderservice;

import java.util.*;

/**
 * Simulates business logic for orders: placement, update, cancel, inventory decrement, etc.
 */
public class OrderService {

    // Possible order status values
    public enum Status { PENDING, SHIPPED, DELIVERED, CANCELLED, FAILED }

    // Internal state: orderId → OrderRecord (holds order and status)
    private final Map<String, OrderRecord> orderMap = new TreeMap<>(String::compareTo);

    // Simulated inventory (productId → quantity)
    private final Map<String, Integer> inventory = new HashMap<>();

    // -- Core model for order state --
    public static class OrderRecord {
        private final Order order;
        private Status status;
        public OrderRecord(Order o, Status s) { this.order = o; this.status = s; }
        public Order getOrder() { return order; }
        public Status getStatus() { return status; }
        public void setStatus(Status s) { this.status = s; }
        public int getQuantity() { return order.getQuantity(); }
    }

    /**
     * Setup: Add/override stock for a product directly (used only for setup and testing).
     */
    public void setProductStock(String productId, int quantity) {
        inventory.put(productId, quantity);
    }

    /**
     * Attempts to place a new order.
     * - Returns true if order is placed and inventory decremented.
     * - Returns false if stock unavailable or invalid.
     */
    public boolean placeOrder(String orderId, String productId, int quantity) {
        if (orderId == null || orderId.isEmpty()) throw new IllegalArgumentException("Order ID required");
        if (productId == null || productId.isEmpty()) throw new IllegalArgumentException("Product ID required");
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");
        if (orderMap.containsKey(orderId)) throw new IllegalStateException("Duplicate order id");

        int stock = inventory.getOrDefault(productId, 0);
        if (stock < quantity) return false;

        Order order = new Order(orderId, productId, quantity);
        orderMap.put(orderId, new OrderRecord(order, Status.PENDING));
        inventory.put(productId, stock - quantity);
        return true;
    }

    /**
     * Cancel an order: restores inventory and sets CANCELLED.
     */
    public boolean cancelOrder(String orderId) {
        OrderRecord rec = orderMap.get(orderId);
        if (rec == null) return false;
        if (rec.status == Status.CANCELLED) return false;
        // Restore inventory
        String productId = rec.order.getProduct();
        int qty = rec.order.getQuantity();
        inventory.put(productId, inventory.getOrDefault(productId, 0) + qty);
        rec.status = Status.CANCELLED;
        return true;
    }

    /**
     * Update the status for an order (e.g. PENDING->SHIPPED->DELIVERED).
     */
    public boolean updateStatus(String orderId, Status newStatus) {
        OrderRecord rec = orderMap.get(orderId);
        if (rec == null) return false;
        if (newStatus == null) return false;
        // Prohibit invalid status transitions
        if (rec.status == Status.CANCELLED) return false;
        if (rec.status == Status.DELIVERED && newStatus != Status.DELIVERED) return false;
        rec.status = newStatus;
        return true;
    }

    /**
     * Returns the order record, or null if not found.
     */
    public OrderRecord getOrderRecord(String orderId) {
        return orderMap.get(orderId);
    }

    /**
     * Lists all current product stock levels.
     */
    public Map<String, Integer> getInventory() {
        return new HashMap<>(inventory);
    }

    /**
     * Clear all recorded orders/inventory (for test resets).
     */
    public void reset() {
        orderMap.clear();
        inventory.clear();
    }
}
