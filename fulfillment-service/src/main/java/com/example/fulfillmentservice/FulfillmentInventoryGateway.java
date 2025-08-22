package com.example.fulfillmentservice;

import com.example.fulfillmentservice.model.OrderEvent;
import org.springframework.stereotype.Component;

/**
 * Integrates with Inventory Service (via Kafka or REST).
 * For now, simulates a successful allocation; extend to real inventory service as needed.
 */
@Component
public class FulfillmentInventoryGateway {

    // Simulate async inventory allocation (always succeeds—extend for real calls)
    public boolean allocate(OrderEvent order) {
        // Integration point: replace with REST client or Kafka producer/consumer for real service
        return true;
    }

    // Simulate inventory rollback/compensation (for saga failure)
    public boolean rollback(OrderEvent order) {
        // Integration point: send compensation/cancel message to inventory service
        return true;
    }
}
