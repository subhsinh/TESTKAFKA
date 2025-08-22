package com.example.fulfillmentservice;


import com.example.fulfillmentservice.model.*;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class FulfillmentSagaOrchestratorTest {

    private FulfillmentSagaOrchestrator orchestrator;
    private StubKafkaTemplate kafkaTemplate;
    private FulfillmentInventoryGateway inventoryGateway;

    // Does not extend KafkaTemplate; just provides matching send() signature.
    static class StubKafkaTemplate {
        public List<String> sent = new java.util.ArrayList<>();
        public java.util.concurrent.CompletableFuture send(String topic, String key, String value) {
            sent.add(value);
            return java.util.concurrent.CompletableFuture.completedFuture(null);
        }
    }

    static class TestInventoryGateway extends FulfillmentInventoryGateway {
        private boolean allocationSuccess = true;
        public void setAllocationSuccess(boolean val) { this.allocationSuccess = val; }
        @Override public boolean allocate(OrderEvent order) { return allocationSuccess; }
        @Override public boolean rollback(OrderEvent order) { return true; }
    }

    @BeforeEach
    void setUp() {
        kafkaTemplate = new StubKafkaTemplate();
        inventoryGateway = new TestInventoryGateway();
        orchestrator = new FulfillmentSagaOrchestrator(
            (topic, key, value) -> kafkaTemplate.send(topic, key, value),
            inventoryGateway
        );
    }

    @Test
    void sagaProcessesEventsEndToEnd() {
        OrderEvent order = new OrderEvent("O1", "SKU42", 3, "C7", java.time.Instant.now());
        String orderJson = KafkaSerdeUtil.toJson(order);
        System.out.println("[TEST] orderJson: " + orderJson);
        // Print a round-trip parse for confirmation
        OrderEvent roundTrip = KafkaSerdeUtil.fromJson(orderJson, OrderEvent.class);
        System.out.println("[TEST] Round-trip orderId: " + (roundTrip == null ? null : roundTrip.getOrderId()));
        orchestrator.onOrderPlaced(orderJson);

        List<FulfillmentEvent> eventLog = orchestrator.getEventLog("O1");
        System.out.println("[TEST] eventLog size after saga: " + eventLog.size());
        for (FulfillmentEvent evt : eventLog) {
            System.out.println("[TEST] Event: " + evt.getType() + ", Status: " + evt.getStatus());
        }
        assertThat(eventLog).hasSizeGreaterThanOrEqualTo(3);
        assertThat(eventLog.get(0).getType()).isEqualTo("OrderPlaced");
        assertThat(eventLog.get(1).getType()).isEqualTo("AllocationRequested");
        assertThat(eventLog.get(2).getType())
                .isIn("AllocationSucceeded", "ShippingDone");

        FulfillmentStatus status = orchestrator.getCurrentStatus("O1");
        System.out.println("[TEST] currentStatus: " + status);
        assertThat(status).isIn(FulfillmentStatus.SHIPPED, FulfillmentStatus.ALLOCATED);
    }

    @Test
    void sagaTriggersCompensationOnFail() {
        ((TestInventoryGateway)inventoryGateway).setAllocationSuccess(false);

        orchestrator = new FulfillmentSagaOrchestrator(
            (topic, key, value) -> kafkaTemplate.send(topic, key, value),
            inventoryGateway
        );
        OrderEvent order = new OrderEvent("FAIL2", "SKU99", 1, "C9", java.time.Instant.now());
        String orderJson = KafkaSerdeUtil.toJson(order);
        System.out.println("[TEST] orderJson: " + orderJson);
        // Print a round-trip parse for confirmation
        OrderEvent roundTrip = KafkaSerdeUtil.fromJson(orderJson, OrderEvent.class);
        System.out.println("[TEST] Round-trip orderId: " + (roundTrip == null ? null : roundTrip.getOrderId()));
        orchestrator.onOrderPlaced(orderJson);

        List<FulfillmentEvent> eventLog = orchestrator.getEventLog("FAIL2");
        System.out.println("[TEST] eventLog size after compensation: " + eventLog.size());
        for (FulfillmentEvent evt : eventLog) {
            System.out.println("[TEST] Event: " + evt.getType() + ", Status: " + evt.getStatus());
        }
        assertThat(eventLog.stream().anyMatch(e -> e.getStatus() == FulfillmentStatus.FAILED)).isTrue();
        assertThat(eventLog.stream().anyMatch(e -> e.getStatus() == FulfillmentStatus.COMPENSATED)).isTrue();
    }
}
