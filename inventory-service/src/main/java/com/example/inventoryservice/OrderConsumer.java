package com.example.inventoryservice;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Kafka Consumer for "orders" topic that updates inventory (demonstrated by a log).
 */
@Service
public class OrderConsumer {

    @KafkaListener(topics = "orders", groupId = "inventory-group")
    public void consumeOrder(Order order) {
        System.out.println("Received Order: " + order.getId() +
            ", product: " + order.getProduct() +
            ", quantity: " + order.getQuantity());
        // Simulate inventory logic (could update a DB, etc.)
    }
}
