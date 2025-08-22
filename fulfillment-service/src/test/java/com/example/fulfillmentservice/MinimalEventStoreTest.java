package com.example.fulfillmentservice;


import com.example.fulfillmentservice.model.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

class MinimalEventStoreTest {
    static class DummySender implements EventSender {
        @Override public CompletableFuture<?> send(String topic, String key, String value) { return CompletableFuture.completedFuture(null); }
    }

    @Test
    void canAppendAndRetrieveEvents() {
        // Directly test Jackson serialization of OrderEvent
        var testEvt = new com.example.fulfillmentservice.model.OrderEvent("T1", "P1", 4, "C1", Instant.now());
        System.out.println("[JACKSON TEST] declared fields:");
        for (var f : testEvt.getClass().getDeclaredFields()) {
            System.out.println(" - " + f.getName());
        }
        String json = com.example.fulfillmentservice.KafkaSerdeUtil.toJson(testEvt);
        System.out.println("[JACKSON TEST] json: " + json);
        var parsed = com.example.fulfillmentservice.KafkaSerdeUtil.fromJson(json, com.example.fulfillmentservice.model.OrderEvent.class);
        System.out.println("[JACKSON TEST] orderId: " + (parsed == null ? null : parsed.getOrderId()));

        FulfillmentSagaOrchestrator orch = new FulfillmentSagaOrchestrator(new DummySender(), new FulfillmentInventoryGateway());
        var evt = new FulfillmentEvent("evt1", "myOrder", FulfillmentStatus.NEW, "OrderPlaced", "{}", Instant.now(), "myOrder", null);
        orch.getEventLog("myOrder"); // should be empty
        orch.getCurrentStatus("myOrder"); // should be null
        orch.getEventLog("otherOrder"); // should be empty

        // Append event via internal call
        orch.getClass().getDeclaredMethods(); // just to keep from strip
        // Use reflection to call private appendAndPublishEvent
        try {
            var m = FulfillmentSagaOrchestrator.class.getDeclaredMethod("appendAndPublishEvent", FulfillmentEvent.class);
            m.setAccessible(true);
            m.invoke(orch, evt);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        List<FulfillmentEvent> events = orch.getEventLog("myOrder");
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getType()).isEqualTo("OrderPlaced");
        FulfillmentStatus st = orch.getCurrentStatus("myOrder");
        assertThat(st).isEqualTo(FulfillmentStatus.NEW);
    }
}
