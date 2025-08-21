package com.example.inventoryservice;

import java.util.HashMap;
import java.util.Map;

/**
 * Critical business logic for an Amazon-style inventory system.
 */
public class InventoryService {
    // Simulate a product catalog (productId → Inventory)
    private final Map<String, Inventory> inventoryMap = new HashMap<>();

    /**
     * Add a new product to inventory.
     */
    public synchronized void addProduct(String productId, String productName, int stock) {
        if (inventoryMap.containsKey(productId)) throw new IllegalStateException("Product already exists");
        inventoryMap.put(productId, new Inventory(productId, productName, stock));
    }

    /**
     * Attempt to reserve/allocate stock for an order.
     * Returns true if successful, false if out of stock.
     */
    public synchronized boolean reserveStock(String productId, int qty) {
        Inventory inv = inventoryMap.get(productId);
        if (inv == null) throw new IllegalArgumentException("Product not found");
        try {
            inv.decrementStock(qty);
            return true;
        } catch (IllegalStateException e) {
            return false; // oversell, out of stock
        }
    }

    /**
     * Cancel reservation—add stock back.
     */
    public synchronized void cancelReservation(String productId, int qty) {
        Inventory inv = inventoryMap.get(productId);
        if (inv == null) throw new IllegalArgumentException("Product not found");
        inv.incrementStock(qty);
    }

    /**
     * Restock product (e.g., from a supplier).
     */
    public synchronized void restock(String productId, int qty) {
        Inventory inv = inventoryMap.get(productId);
        if (inv == null) throw new IllegalArgumentException("Product not found");
        inv.incrementStock(qty);
    }

    /**
     * Return available stock.
     */
    public synchronized int getStock(String productId) {
        Inventory inv = inventoryMap.get(productId);
        if (inv == null) throw new IllegalArgumentException("Product not found");
        return inv.getStock();
    }

    /**
     * Used for advanced tests: Returns the internal inventory mapping.
     */
    Map<String, Inventory> getInventoryMap() {
        return inventoryMap;
    }
}
