package com.example.fulfillmentservice;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.example.fulfillmentservice.model.OrderEvent;

/**
 * Integrates with Inventory Service (via Kafka or REST).
 * For now, uses an in-memory product inventory map and validates stock before allocation.
 */
@Component
public class FulfillmentInventoryGateway {

    // In-memory inventory: product name -> quantity available
    private final Map<String, Integer> inventory = new ConcurrentHashMap<>();

    public FulfillmentInventoryGateway() {
        // Initialize with some sample inventory (customize as needed)
        inventory.put("notebook", 10);
        inventory.put("phone", 50);
        inventory.put("pen", 200);
    }

    // Allocate inventory for the given order (returns true if successful, false if not enough stock)
    public boolean allocate(OrderEvent order) {
        // Defensive checks
        if (order == null || order.getProduct() == null) return false;
        inventory.putIfAbsent(order.getProduct(), 0);
        synchronized (inventory) { // simple sync; tune for perf in prod
            int available = inventory.get(order.getProduct());
            if (available >= order.getQuantity()) {
                inventory.put(order.getProduct(), available - order.getQuantity());
                System.out.println("[DEBUG] Inventory allocated: " + order.getQuantity() + " " + order.getProduct() +
                        " left=" + inventory.get(order.getProduct()));
                return true;
            } else {
                System.out.println("[DEBUG] Allocation FAILED: only " + available + " " + order.getProduct() +
                        " left, needed " + order.getQuantity());
                return false;
            }
        }
    }

    // Rollback inventory allocation (for saga compensation)
    public boolean rollback(OrderEvent order) {
        if (order == null || order.getProduct() == null) return false;
        synchronized (inventory) {
            int current = inventory.getOrDefault(order.getProduct(), 0);
            inventory.put(order.getProduct(), current + order.getQuantity());
            System.out.println("[DEBUG] Inventory rolled back: +" + order.getQuantity() + " " + order.getProduct() +
                    " new total=" + inventory.get(order.getProduct()));
            return true;
        }
    }

    // For testing/inspection: get current stock for a product
    public int getStock(String product) {
        return inventory.getOrDefault(product, 0);
    }
}
