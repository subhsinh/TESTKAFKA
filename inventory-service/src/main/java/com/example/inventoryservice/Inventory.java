package com.example.inventoryservice;

/**
 * Domain model for inventory item (e.g., a SKU in a warehouse).
 */
public class Inventory {
    private String productId;
    private String productName;
    private int stock;

    public Inventory() { }

    public Inventory(String productId, String productName, int stock) {
        this.productId = productId;
        this.productName = productName;
        this.stock = stock;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public void decrementStock(int qty) {
        if (qty <= 0) throw new IllegalArgumentException("Quantity must be positive");
        if (stock < qty) throw new IllegalStateException("Insufficient stock");
        stock -= qty;
    }

    public void incrementStock(int qty) {
        if (qty <= 0) throw new IllegalArgumentException("Quantity must be positive");
        stock += qty;
    }
}
