package com.example.orderservice;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderTest {
    @Test
    void testOrderGettersSetters() {
        Order order = new Order();
        order.setId("id1");
        order.setProduct("Pen");
        order.setQuantity(5);

        assertEquals("id1", order.getId());
        assertEquals("Pen", order.getProduct());
        assertEquals(5, order.getQuantity());
    }

    @Test
    void testOrderAllArgsConstructor() {
        Order order = new Order("id2", "Book", 3);

        assertEquals("id2", order.getId());
        assertEquals("Book", order.getProduct());
        assertEquals(3, order.getQuantity());
    }
}
