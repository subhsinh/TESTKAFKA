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
        if (productId == null || productId.trim().isEmpty())
            throw new IllegalArgumentException("Product ID must not be empty/null");
        if (productName == null || productName.trim().isEmpty())
            throw new IllegalArgumentException("Product Name must not be empty/null");
        if (stock < 0)
            throw new IllegalArgumentException("Stock must be zero or positive");
        // Allow duplicates with additive quantity (as per new test spec)
        if (inventoryMap.containsKey(productId)) {
            inventoryMap.get(productId).incrementStock(stock);
        } else {
            inventoryMap.put(productId, new Inventory(productId, productName, stock));
        }
    }

    /**
     * Attempt to reserve/allocate stock for an order.
     * Returns true if successful, false if out of stock.
     */
    public synchronized boolean reserveStock(String productId, int qty) {
        if (productId == null || productId.trim().isEmpty())
            throw new IllegalArgumentException("Product ID must not be empty/null");
        if (qty <= 0)
            throw new IllegalArgumentException("Quantity must be positive");
        Inventory inv = inventoryMap.get(productId);
        if (inv == null) throw new IllegalArgumentException("Product not found");
        try {
            inv.decrementStock(qty);
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }

    /**
     * Cancel reservation—add stock back.
     */
    public synchronized void cancelReservation(String productId, int qty) {
        if (productId == null || productId.trim().isEmpty())
            throw new IllegalArgumentException("Product ID must not be empty/null");
        if (qty <= 0)
            throw new IllegalArgumentException("Quantity must be positive");
        Inventory inv = inventoryMap.get(productId);
        if (inv == null) throw new IllegalArgumentException("Product not found");
        inv.incrementStock(qty);
    }

    /**
     * Restock product (e.g., from a supplier).
     */
    public synchronized void restock(String productId, int qty) {
        if (productId == null || productId.trim().isEmpty())
            throw new IllegalArgumentException("Product ID must not be empty/null");
        if (qty <= 0)
            throw new IllegalArgumentException("Quantity must be positive");
        Inventory inv = inventoryMap.get(productId);
        if (inv == null) throw new IllegalArgumentException("Product not found");
        inv.incrementStock(qty);
    }

    /**
     * Return available stock.
     */
    public synchronized int getStock(String productId) {
        if (productId == null || productId.trim().isEmpty())
            throw new IllegalArgumentException("Product ID must not be empty/null");
        Inventory inv = inventoryMap.get(productId);
        if (inv == null) return 0;
        return inv.getStock();
    }

    /**
     * Used for advanced tests: Returns the internal inventory mapping.
     */
    Map<String, Inventory> getInventoryMap() {
        return inventoryMap;
    }
}
