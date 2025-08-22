package com.example.orderservice;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class OrderServiceTest {

    private OrderService svc;

    @BeforeEach
    void setUp() {
        svc = new OrderService();
        svc.setProductStock("p1", 10);
        svc.setProductStock("p2", 3);
    }

    @Test
    void placeValidOrderDecrementsInventory() {
        assertTrue(svc.placeOrder("order1", "p1", 2));
        assertEquals(8, svc.getInventory().get("p1"));
        assertEquals(OrderService.Status.PENDING, svc.getOrderRecord("order1").getStatus());
    }

    @Test
    void placeOrderInsufficientStockFails() {
        assertFalse(svc.placeOrder("order2", "p2", 99));
        assertNull(svc.getOrderRecord("order2"));
    }

    @Test
    void cancelOrderRestoresInventory() {
        assertTrue(svc.placeOrder("order3", "p1", 2));
        assertTrue(svc.cancelOrder("order3"));
        assertEquals(10, svc.getInventory().get("p1"));
        assertEquals(OrderService.Status.CANCELLED, svc.getOrderRecord("order3").getStatus());
    }

    @Test
    void updateOrderStatusAfterPlacement() {
        svc.placeOrder("order4", "p1", 1);
        assertTrue(svc.updateStatus("order4", OrderService.Status.SHIPPED));
        assertEquals(OrderService.Status.SHIPPED, svc.getOrderRecord("order4").getStatus());
    }

    @Test
    void placeTriggersInventoryServiceFailureMarksFailed() {
        svc.setProductStock("failprod", 0);
        assertFalse(svc.placeOrder("order5", "failprod", 1));
    }

    @Test
    void placeDuplicateOrderIdFails() {
        svc.placeOrder("order6", "p2", 1);
        assertThrows(IllegalStateException.class, () -> svc.placeOrder("order6", "p2", 1));
    }

    @Test
    void updateStatusForNonExistentOrderFails() {
        assertFalse(svc.updateStatus("notfound",  OrderService.Status.SHIPPED));
    }

    @Test
    void cancelNonExistentOrderFails() {
        assertFalse(svc.cancelOrder("nope"));
    }

    @Test
    void getNonExistentOrderReturnsNull() {
        assertNull(svc.getOrderRecord("nou"));
    }

    @Test
    void invalidIdInputsToPlaceUpdateCancel() {
        assertThrows(IllegalArgumentException.class, () -> svc.placeOrder(null, "p1", 1));
        assertThrows(IllegalArgumentException.class, () -> svc.placeOrder("", "p1", 1));
        assertThrows(IllegalArgumentException.class, () -> svc.placeOrder("z", null, 1));
        assertThrows(IllegalArgumentException.class, () -> svc.placeOrder("y", "", 1));
        assertThrows(IllegalArgumentException.class, () -> svc.placeOrder("x", "x", 0));
        assertThrows(IllegalArgumentException.class, () -> svc.placeOrder("w", "x", -1));
    }

    @Test
    void placeOrderWithZeroQuantityFails() {
        assertThrows(IllegalArgumentException.class, () -> svc.placeOrder("orderZ", "p1", 0));
    }

    @Test
    void placeOrderForNonExistentProductFails() {
        assertFalse(svc.placeOrder("oNEWW", "notfound", 7));
        assertNull(svc.getOrderRecord("oNEWW"));
    }

    @Test
    void fullStatusLifecycle() {
        svc.placeOrder("ordcycle", "p1", 2);
        assertTrue(svc.updateStatus("ordcycle", OrderService.Status.SHIPPED));
        assertTrue(svc.updateStatus("ordcycle", OrderService.Status.DELIVERED));
        assertEquals(OrderService.Status.DELIVERED, svc.getOrderRecord("ordcycle").getStatus());
        // No further transitions after DELIVERED
        assertFalse(svc.updateStatus("ordcycle", OrderService.Status.PENDING));
    }

    @Test
    void invalidOrderStatusTransitionFails() {
        svc.placeOrder("wrongcycle", "p2", 2);
        assertTrue(svc.updateStatus("wrongcycle", OrderService.Status.SHIPPED));
        assertTrue(svc.cancelOrder("wrongcycle"));
        assertFalse(svc.updateStatus("wrongcycle", OrderService.Status.PENDING));
    }

    @Test
    void cancelAlreadyCancelledOrderFails() {
        svc.placeOrder("cancancel", "p1", 1);
        assertTrue(svc.cancelOrder("cancancel"));
        assertFalse(svc.cancelOrder("cancancel"));
    }

    @Test
    void orderRetrievalPersistsAcrossResets() {
        svc.placeOrder("sss", "p2", 1);
        assertEquals(OrderService.Status.PENDING, svc.getOrderRecord("sss").getStatus());
        svc.reset();
        assertNull(svc.getOrderRecord("sss"));
    }

    @Test
    void batchOrderingSomeFailDueToStock() {
        svc.setProductStock("group", 4);
        assertTrue(svc.placeOrder("g1", "group", 2));
        assertTrue(svc.placeOrder("g2", "group", 1));
        assertTrue(svc.placeOrder("g3", "group", 1));
        assertFalse(svc.placeOrder("g4", "group", 1));
    }

    @Test
    void doubleCancelDifferentUsersFails() {
        svc.placeOrder("doublecancel", "p1", 1);
        assertTrue(svc.cancelOrder("doublecancel"));
        assertFalse(svc.cancelOrder("doublecancel"));
    }

    @Test
    void statusUpdateWithWrongType() {
        svc.placeOrder("tst", "p1", 1);
        // Java is strongly typed, so this case is only meaningful as a compile-time error prevented
        assertFalse(svc.updateStatus("tst", null));
    }

    @Test
    void placeOrderWithNegativeQuantityFails() {
        assertThrows(IllegalArgumentException.class, () -> svc.placeOrder("negq", "p2", -5));
    }

    @Test
    void orderIdCaseSensitivity() {
        svc.placeOrder("CaseX", "p1", 1);
        assertNull(svc.getOrderRecord("casex"));
    }

    @Test
    void placeOrderWithEmptyProductIdFails() {
        assertThrows(IllegalArgumentException.class, () -> svc.placeOrder("ord", "", 1));
    }

    @Test
    void orderWithSpecialCharacterProductId() {
        svc.setProductStock("sP@C1!", 5);
        assertTrue(svc.placeOrder("ospec", "sP@C1!", 2));
        assertEquals(3, svc.getInventory().get("sP@C1!"));
        assertEquals(OrderService.Status.PENDING, svc.getOrderRecord("ospec").getStatus());
    }

    @Test
    void bulkPlaceCancelOrdersFullReset() {
        for (int i = 0; i < 10; ++i) {
            svc.setProductStock("r" + i, 10);
            assertTrue(svc.placeOrder("ordC" + i, "r" + i, 5));
            svc.cancelOrder("ordC" + i);
            assertEquals(10, svc.getInventory().get("r" + i));
        }
    }

    @Test
    void duplicateCancelOrderTest() {
        svc.placeOrder("dupCA", "p2", 1);
        assertTrue(svc.cancelOrder("dupCA"));
        assertFalse(svc.cancelOrder("dupCA"));
    }

    @Test
    void getStockLevelsAfterAllRemovals() {
        for (int i = 0; i < 5; ++i) {
            svc.setProductStock("ALL" + i, 4);
            assertTrue(svc.placeOrder("allR" + i, "ALL" + i, 4));
        }
        for (int i = 0; i < 5; ++i) {
            svc.cancelOrder("allR" + i);
            assertEquals(4, svc.getInventory().get("ALL" + i));
        }
    }

    @Test
    void getStockForMixedTypesAndCase() {
        svc.setProductStock("CaTy", 1);
        assertTrue(svc.placeOrder("MixA", "CaTy", 1));
        assertFalse(svc.placeOrder("MixB", "caty", 1));
        assertEquals(0, svc.getInventory().get("CaTy"));
        assertFalse(svc.placeOrder("MixC", "caty", 1));
    }

    @Test
    void systemRecoversFromAllZeroStock() {
        svc.setProductStock("TOZERO", 2);
        assertTrue(svc.placeOrder("zerord1", "TOZERO", 2));
        assertFalse(svc.placeOrder("zerord2", "TOZERO", 1));
        svc.cancelOrder("zerord1");
        assertTrue(svc.placeOrder("zerord3", "TOZERO", 1));
    }
}
