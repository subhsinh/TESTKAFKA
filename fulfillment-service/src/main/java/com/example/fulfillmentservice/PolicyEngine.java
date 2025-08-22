package com.example.fulfillmentservice;

import com.example.fulfillmentservice.model.OrderEvent;
import org.springframework.stereotype.Component;

/**
 * Policy engine for dynamic rule-based fulfillment logic.
 *
 * In a full product, this might be backed by Drools, easy-rules, or remote rules store.
 */
@Component
public class PolicyEngine {

    // Evaluate if an order/event is eligible for fulfillment
    public boolean evaluate(OrderEvent order) {
        // Example: block if product is "restricted"
        if ("RESTRICTED".equalsIgnoreCase(order.getProductId())) return false;
        // Future: more rules, dynamic from Kafka/config/db
        return true;
    }
}
