package com.example.fulfillmentservice;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.example.fulfillmentservice.model.FulfillmentEvent;
import com.example.fulfillmentservice.model.FulfillmentStatus;
import com.example.fulfillmentservice.model.OrderEvent;

/**
 * Core business orchestrator for distributed, event-sourced order fulfillment with saga/compensation logic.
 */
@Component
public class FulfillmentSagaOrchestrator {

    private final EventSender eventSender;
    private final FulfillmentInventoryGateway inventoryGateway;
    // eventStore backed by disk for idempotence across restarts
    private final Map<String, List<FulfillmentEvent>> eventStore = EventStoreDiskPersistence.load();

    @Value("${spring.kafka.template.default-topic:fulfillment-events}")
    private String fulfillmentTopic;

    // Event/state store for quick lookup (could be replaced by DB/event store)
    private final Map<String, FulfillmentStatus> currentStatus = new ConcurrentHashMap<>();

    public FulfillmentSagaOrchestrator(EventSender eventSender,
                                       FulfillmentInventoryGateway inventoryGateway) {
        this.eventSender = eventSender;
        this.inventoryGateway = inventoryGateway;
    }

@KafkaListener(topics = "orders", groupId = "fulfillment-service-group")
public void onOrderPlaced(String orderJson) {
    System.out.println("[DEBUG] onOrderPlaced invoked with: " + orderJson);
    // Parse JSON to OrderEvent (later: use ObjectMapper bean)
    OrderEvent order = KafkaSerdeUtil.fromJson(orderJson, OrderEvent.class);
    if (order == null || order.getOrderId() == null) {
        System.out.println("[DEBUG] Invalid or null order parsed, aborting.");
        return;
    }

    // Block duplicate orderId unless only the very first OrderPlaced event is processed ever for an orderId
    if (eventStore.containsKey(order.getOrderId())) {
        System.out.println("[DEBUG] Duplicate orderId detected (" + order.getOrderId() + "), ignoring new placement.");
        return;
    }

    // Begin fulfillment process (saga)
    FulfillmentEvent evt = new FulfillmentEvent(
        UUID.randomUUID().toString(),
        order.getOrderId(),
        FulfillmentStatus.NEW,
        "OrderPlaced",
        orderJson,
        Instant.now(),
        order.getOrderId(), // correlationId is orderId for now
        null
    );
    appendAndPublishEvent(evt);

    // Move to allocation
    allocateInventory(order);
}

    private void allocateInventory(OrderEvent order) {
        System.out.println("[DEBUG] allocateInventory for: " + order.getOrderId());
        // Set status to ALLOCATING
        FulfillmentEvent evt = new FulfillmentEvent(
            UUID.randomUUID().toString(),
            order.getOrderId(),
            FulfillmentStatus.ALLOCATING,
            "AllocationRequested",
            KafkaSerdeUtil.toJson(order),
            Instant.now(),
            order.getOrderId(),
            null
        );
        appendAndPublishEvent(evt);

        boolean allocationSuccess = inventoryGateway.allocate(order);
        if (allocationSuccess) {
            FulfillmentEvent allocEvt = new FulfillmentEvent(
                UUID.randomUUID().toString(),
                order.getOrderId(),
                FulfillmentStatus.ALLOCATED,
                "AllocationSucceeded",
                KafkaSerdeUtil.toJson(order),
                Instant.now(),
                order.getOrderId(),
                null
            );
            appendAndPublishEvent(allocEvt);

            // Next: simulate shipping
            fulfillOrder(order.getOrderId(), FulfillmentStatus.SHIPPED, "ShippingDone");
        } else {
            FulfillmentEvent failEvt = new FulfillmentEvent(
                UUID.randomUUID().toString(),
                order.getOrderId(),
                FulfillmentStatus.FAILED,
                "AllocationFailed",
                KafkaSerdeUtil.toJson(order),
                Instant.now(),
                order.getOrderId(),
                null
            );
            appendAndPublishEvent(failEvt);
            // Saga compensation: emit rollback
            inventoryGateway.rollback(order);
            FulfillmentEvent compEvt = new FulfillmentEvent(
                UUID.randomUUID().toString(),
                order.getOrderId(),
                FulfillmentStatus.COMPENSATED,
                "AllocationRolledBack",
                KafkaSerdeUtil.toJson(order),
                Instant.now(),
                order.getOrderId(),
                order.getOrderId()
            );
            appendAndPublishEvent(compEvt);
        }
    }

    private void fulfillOrder(String orderId, FulfillmentStatus terminalStatus, String eventType) {
        FulfillmentEvent evt = new FulfillmentEvent(
            UUID.randomUUID().toString(),
            orderId,
            terminalStatus,
            eventType,
            null,
            Instant.now(),
            orderId,
            null
        );
        appendAndPublishEvent(evt);
    }

    private synchronized void appendAndPublishEvent(FulfillmentEvent evt) {
        // Store to disk-backed log
        eventStore.computeIfAbsent(evt.getOrderId(), k -> new ArrayList<>()).add(evt);
        EventStoreDiskPersistence.save(eventStore);
        currentStatus.put(evt.getOrderId(), evt.getStatus());
        System.out.println("[DEBUG] Event appended: " + evt.getType() + " for " + evt.getOrderId() +
                " | EventStore size=" + eventStore.get(evt.getOrderId()).size() +
                ", Current status=" + evt.getStatus());
        // Send to Kafka topic
        eventSender.send(fulfillmentTopic, evt.getOrderId(), KafkaSerdeUtil.toJson(evt));
    }

    // For REST/API: retrieve fulfillment state and event log
    public List<FulfillmentEvent> getEventLog(String orderId) {
        return eventStore.getOrDefault(orderId, Collections.emptyList());
    }

    public FulfillmentStatus getCurrentStatus(String orderId) {
        return currentStatus.get(orderId);
    }
}
