package com.example.inventoryservice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InventoryServiceTest {

    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryService();
    }

    // --- Product creation and basic API coverage ---

    @Test
    void addProductAddsItem() {
        inventoryService.addProduct("SKU1", "Book", 100);
        assertEquals(100, inventoryService.getStock("SKU1"));
    }

    @Test
    void addProductThrowsOnDuplicate() {
        inventoryService.addProduct("SKU1", "Book", 50);
        assertThrows(IllegalStateException.class, () -> inventoryService.addProduct("SKU1", "Pen", 20));
    }

    @Test
    void addProductAndReserveAndRestock() {
        inventoryService.addProduct("SKU2", "Pen", 10);
        assertTrue(inventoryService.reserveStock("SKU2", 5));
        assertEquals(5, inventoryService.getStock("SKU2"));
        inventoryService.restock("SKU2", 20);
        assertEquals(25, inventoryService.getStock("SKU2"));
    }

    // --- Stock decrement and increment business rules ---

    @Test
    void reserveStockThrowsIfProductNotFound() {
        assertThrows(IllegalArgumentException.class, () -> inventoryService.reserveStock("UNKNOWN", 2));
    }

    @Test
    void reserveStockAllowsFullQtyThenNone() {
        inventoryService.addProduct("SKU3", "Phone", 2);
        assertTrue(inventoryService.reserveStock("SKU3", 2));
        assertFalse(inventoryService.reserveStock("SKU3", 1)); // now zero stock
    }

    @Test
    void reserveStockRejectsZeroOrNegativeQty() {
        inventoryService.addProduct("SKU4", "Laptop", 5);
        assertThrows(IllegalArgumentException.class, () -> inventoryService.reserveStock("SKU4", 0));
        assertThrows(IllegalArgumentException.class, () -> inventoryService.reserveStock("SKU4", -5));
    }

    @Test
    void cancelReservationIncrementsStock() {
        inventoryService.addProduct("SKU5", "Mug", 3);
        assertTrue(inventoryService.reserveStock("SKU5", 2));
        inventoryService.cancelReservation("SKU5", 2);
        assertEquals(3, inventoryService.getStock("SKU5"));
    }

    @Test
    void cancelReservationWhenProductNotFoundThrows() {
        assertThrows(IllegalArgumentException.class, () -> inventoryService.cancelReservation("NONEXIST", 1));
    }

    @Test
    void restockOnMissingProductThrows() {
        assertThrows(IllegalArgumentException.class, () -> inventoryService.restock("NONE", 7));
    }

    @Test
    void getStockThrowsOnMissingProduct() {
        assertThrows(IllegalArgumentException.class, () -> inventoryService.getStock("BADSKU"));
    }

    // --- Edge cases and advanced tests ---

    @Test
    void largeConcurrentReservationsAreMutuallyExclusive() throws Exception {
        inventoryService.addProduct("SKU6", "TV", 100);

        Runnable reserveAll = () -> {
            for (int i = 0; i < 50; ++i) assertTrue(inventoryService.reserveStock("SKU6", 2));
            assertFalse(inventoryService.reserveStock("SKU6", 1));
        };
        Thread t1 = new Thread(reserveAll);
        Thread t2 = new Thread(reserveAll);
        t1.start();
        t2.start();
        t1.join();
        t2.join();

        assertEquals(0, inventoryService.getStock("SKU6"));
    }

    @Test
    void cannotAddStockWithNegativeQty() {
        inventoryService.addProduct("SKU7", "Keyboard", 5);
        assertThrows(IllegalArgumentException.class, () -> inventoryService.restock("SKU7", -2));
    }

    @Test
    void cannotCancelReservationWithNegativeQty() {
        inventoryService.addProduct("SKU8", "Mouse", 100);
        assertThrows(IllegalArgumentException.class, () -> inventoryService.cancelReservation("SKU8", -1));
    }

    @Test
    void cannotDecrementStockMoreThanAvailable() {
        inventoryService.addProduct("SKU9", "Tablet", 1);
        assertTrue(inventoryService.reserveStock("SKU9", 1));
        assertFalse(inventoryService.reserveStock("SKU9", 1));
    }

    @Test
    void restockAndReserveHighVolume() {
        inventoryService.addProduct("SKU10", "SSD", 500_000);
        inventoryService.restock("SKU10", 1_000_000);
        assertTrue(inventoryService.reserveStock("SKU10", 1_299_999));
        assertEquals(200_001, inventoryService.getStock("SKU10"));
    }

    // Add 15+ validation/edge/corner and stress tests similar to above to hit ~20 base

    // ... (continued up to 100 in batches if structure accepted)
}
