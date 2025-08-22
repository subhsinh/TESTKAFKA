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

    // Legacy test removed: old "addProductThrowsOnDuplicate" logic replaced with quantity increase for duplicate IDs (see addProductWithDuplicateIdIncreasesQuantity)

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
    void getStockReturnsZeroForMissingProductCase() {
        assertEquals(0, inventoryService.getStock("BADSKU"));
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

    @Test
    void addProductWithZeroQuantityAllowed() {
        inventoryService.addProduct("SKUzero", "Eraser", 0);
        assertEquals(0, inventoryService.getStock("SKUzero"));
    }

    @Test
    void addProductNegativeQuantityThrows() {
        assertThrows(IllegalArgumentException.class, () -> inventoryService.addProduct("SKUn1", "Glue", -10));
    }

    @Test
    void addProductWithEmptyIdRaises() {
        assertThrows(IllegalArgumentException.class, () -> inventoryService.addProduct("", "EmptyID", 10));
    }

    @Test
    void addProductWithEmptyNameRaises() {
        assertThrows(IllegalArgumentException.class, () -> inventoryService.addProduct("SKUe", "", 1));
    }

    @Test
    void addProductWithDuplicateIdIncreasesQuantity() {
        inventoryService.addProduct("SKUx", "Book", 5);
        inventoryService.addProduct("SKUx", "Book", 3);
        assertEquals(8, inventoryService.getStock("SKUx"));
    }

    @Test
    void addProductWithSpecialCharsAccepted() {
        inventoryService.addProduct("SPECIAL-!@#", "Special", 4);
        assertEquals(4, inventoryService.getStock("SPECIAL-!@#"));
    }

    @Test
    void addProductCaseSensitivityOnId() {
        inventoryService.addProduct("abc", "A", 2);
        inventoryService.addProduct("ABC", "B", 3);
        assertEquals(2, inventoryService.getStock("abc"));
        assertEquals(3, inventoryService.getStock("ABC"));
    }

    @Test
    void addBulkProductsAndRemove() {
        for (int i = 0; i < 10; ++i)
            inventoryService.addProduct("BULK" + i, "Bulk" + i, 50);
        for (int i = 0; i < 10; ++i)
            assertTrue(inventoryService.reserveStock("BULK" + i, 50));
        for (int i = 0; i < 10; ++i)
            assertEquals(0, inventoryService.getStock("BULK" + i));
    }

    @Test
    void getStockReturnsZeroForMissingItem() {
        assertEquals(0, inventoryService.getStock("DOESNOTEXIST"));
    }

    @Test
    void reduceStockCompletelyToZero() {
        inventoryService.addProduct("OUT", "Toy", 3);
        assertTrue(inventoryService.reserveStock("OUT", 3));
        assertEquals(0, inventoryService.getStock("OUT"));
        assertFalse(inventoryService.reserveStock("OUT", 1));
    }

    @Test
    void reduceStockInsufficientlyRaisesFalse() {
        inventoryService.addProduct("TOOFAR", "Item", 2);
        assertTrue(inventoryService.reserveStock("TOOFAR", 2));
        assertFalse(inventoryService.reserveStock("TOOFAR", 1));
    }

    @Test
    void reduceStockNegativeValueThrows() {
        inventoryService.addProduct("NEGATIVE", "Stapler", 5);
        assertThrows(IllegalArgumentException.class, () -> inventoryService.reserveStock("NEGATIVE", -3));
    }

    @Test
    void reduceStockOnNonExistentProductRaises() {
        assertThrows(IllegalArgumentException.class, () -> inventoryService.reserveStock("NEXIST", 1));
    }

    @Test
    void updateStockForNonExistentProductRaises() {
        assertThrows(IllegalArgumentException.class, () -> inventoryService.restock("XYZ", 6));
    }

    @Test
    void reduceStockEmptyIdRaises() {
        assertThrows(IllegalArgumentException.class, () -> inventoryService.reserveStock("", 2));
    }

    @Test
    void updateStockEmptyIdRaises() {
        assertThrows(IllegalArgumentException.class, () -> inventoryService.restock("", 2));
    }

    @Test
    void updateStockNegativeValueThrows() {
        inventoryService.addProduct("NEGVAL", "Pen", 9);
        assertThrows(IllegalArgumentException.class, () -> inventoryService.restock("NEGVAL", -9));
    }

    @Test
    void addProductWithLargeQuantity() {
        inventoryService.addProduct("LARGE", "BigStack", 10_000_000);
        assertEquals(10_000_000, inventoryService.getStock("LARGE"));
    }

    @Test
    void addAndReduceStockAcrossManyProducts() {
        for (int i = 0; i < 100; ++i)
            inventoryService.addProduct("MIX" + i, "Prod" + i, 100);
        for (int i = 0; i < 100; i += 2)
            assertTrue(inventoryService.reserveStock("MIX" + i, 99));
        for (int i = 0; i < 100; ++i)
            assertTrue(inventoryService.getStock("MIX" + i) == 1 || inventoryService.getStock("MIX" + i) == 100);
    }

    @Test
    void bulkAndCrossProductOperations() {
        inventoryService.addProduct("ONE", "Pn", 5);
        inventoryService.addProduct("TWO", "Pn2", 6);
        assertTrue(inventoryService.reserveStock("ONE", 1));
        assertTrue(inventoryService.reserveStock("TWO", 5));
        inventoryService.cancelReservation("ONE", 1);
        assertEquals(5, inventoryService.getStock("ONE"));
    }

    @Test
    void addProductNonStringIdRaises() {
        assertThrows(IllegalArgumentException.class, () -> inventoryService.addProduct(null, "name", 1));
    }

    @Test
    void reduceStockNonStringIdRaises() {
        assertThrows(IllegalArgumentException.class, () -> inventoryService.reserveStock(null, 1));
    }

    @Test
    void addProductNonStringNameRaises() {
        assertThrows(IllegalArgumentException.class, () -> inventoryService.addProduct("ID101", null, 1));
    }

    // Add-and-check: special char id, empty name/id, recover all zero stock, edge availability after batch remove
    @Test
    void addReduceProductWithSpecialIdChars() {
        inventoryService.addProduct("!hi@1#", "special", 9);
        assertTrue(inventoryService.reserveStock("!hi@1#", 9));
        assertEquals(0, inventoryService.getStock("!hi@1#"));
    }

    @Test
    void stockAvailabilityAfterReductions() {
        inventoryService.addProduct("ITEMZ", "it", 3);
        assertTrue(inventoryService.reserveStock("ITEMZ", 2));
        assertEquals(1, inventoryService.getStock("ITEMZ"));
        assertTrue(inventoryService.reserveStock("ITEMZ", 1));
        assertEquals(0, inventoryService.getStock("ITEMZ"));
    }

    @Test
    void getStockForMixedTypesIsStrictlyIds() {
        inventoryService.addProduct("ABC", "xyz", 1);
        assertEquals(1, inventoryService.getStock("ABC"));
        assertEquals(0, inventoryService.getStock("abc")); // case-sensitive
    }
}
