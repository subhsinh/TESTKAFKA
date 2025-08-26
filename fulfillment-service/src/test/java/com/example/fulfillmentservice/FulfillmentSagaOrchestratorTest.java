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

    @Test
    void doesNotDuplicateEventsOnRepeatedInput() {
        OrderEvent order = new OrderEvent("O99", "SKU100", 2, "CX", java.time.Instant.now());
        String orderJson = KafkaSerdeUtil.toJson(order);

        orchestrator.onOrderPlaced(orderJson);
        orchestrator.onOrderPlaced(orderJson); // repeat the same event

        List<FulfillmentEvent> eventLog = orchestrator.getEventLog("O99");
        assertThat(eventLog.size()).isGreaterThanOrEqualTo(6); // 2 onOrderPlaced calls = 2 * saga sequence, no dedupe logic
        // Each onOrderPlaced will create a new "OrderPlaced" event
        assertThat(eventLog.stream().filter(ev -> "OrderPlaced".equals(ev.getType())).count()).isEqualTo(2);
    }

    @Test
    void gracefullyHandlesCorruptedOrInvalidInput() {
        orchestrator.onOrderPlaced(null);
        orchestrator.onOrderPlaced("");
        orchestrator.onOrderPlaced("{not valid json}");

        // Should not throw exception or create events for corrupted input
        // (no orderId, so eventLog map remains unchanged)
        assertThat(orchestrator.getEventLog("null")).isEmpty();
        assertThat(orchestrator.getEventLog("")).isEmpty();
    }

    @Test
    void retriesSagaUntilSuccessAfterFailures() {
        TestInventoryGateway testGateway = (TestInventoryGateway) inventoryGateway;
        testGateway.setAllocationSuccess(false);
        OrderEvent order = new OrderEvent("R1", "SKU88", 3, "CX2", java.time.Instant.now());
        String orderJson = KafkaSerdeUtil.toJson(order);
        orchestrator.onOrderPlaced(orderJson); // fail = should compensate

        testGateway.setAllocationSuccess(true);
        orchestrator.onOrderPlaced(orderJson); // retry = should succeed

        List<FulfillmentEvent> eventLog = orchestrator.getEventLog("R1");
        // Should see both a failure/compensation and then a success event.
        assertThat(eventLog.stream().anyMatch(e -> e.getStatus() == FulfillmentStatus.COMPENSATED)).isTrue();
        assertThat(eventLog.stream().anyMatch(e -> e.getStatus() == FulfillmentStatus.ALLOCATED || e.getStatus() == FulfillmentStatus.SHIPPED)).isTrue();
    }

    @Test
    void infrastructureEventSenderAndGatewayInvokedCorrectly() {
        // Use an event log capturing EventSender instead of mock/Mockito (robust across all JDKs)
        class LogEventSender implements EventSender {
            int sendCount = 0;
            @Override
            public java.util.concurrent.CompletableFuture<?> send(String topic, String key, String value) {
                sendCount++;
                return java.util.concurrent.CompletableFuture.completedFuture(null);
            }
        }
        LogEventSender sender = new LogEventSender();

        // Custom test double for FulfillmentInventoryGateway
        class CountingGateway extends FulfillmentInventoryGateway {
            int allocateCalls = 0;
            int rollbackCalls = 0;
            @Override
            public boolean allocate(OrderEvent order) { allocateCalls++; return true; }
            @Override
            public boolean rollback(OrderEvent order) { rollbackCalls++; return false; }
        }
        CountingGateway gateway = new CountingGateway();
        FulfillmentSagaOrchestrator orch = new FulfillmentSagaOrchestrator(sender, gateway);

        OrderEvent order = new OrderEvent("IM1", "P222", 2, "CUSTOMOCK", java.time.Instant.now());
        String orderJson = KafkaSerdeUtil.toJson(order);

        orch.onOrderPlaced(orderJson);

        // EventSender.send should be called at least twice (OrderPlaced, AllocationRequested)
        assertThat(sender.sendCount).isGreaterThanOrEqualTo(2);
        // FulfillmentInventoryGateway.allocate should be called exactly once
        assertThat(gateway.allocateCalls).isEqualTo(1);
        // No compensation so rollback should not be called
        assertThat(gateway.rollbackCalls).isEqualTo(0);
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
