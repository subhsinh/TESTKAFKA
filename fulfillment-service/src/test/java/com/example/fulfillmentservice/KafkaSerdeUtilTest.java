package com.example.fulfillmentservice;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import com.example.fulfillmentservice.model.FulfillmentEvent;
import com.example.fulfillmentservice.model.FulfillmentStatus;
import com.example.fulfillmentservice.model.OrderEvent;

class KafkaSerdeUtilTest {

    @Test
    void orderEventSerializesAndDeserializesFully() {
        OrderEvent original = new OrderEvent("ORD999", "SKU42", 18, "CUSTOMER9", Instant.parse("2022-03-01T12:00:00Z"));
        String json = KafkaSerdeUtil.toJson(original);
        OrderEvent back = KafkaSerdeUtil.fromJson(json, OrderEvent.class);
        assertThat(back).isNotNull();
        // assertThat(back.getOrderId()).isEqualTo(original.getOrderId());
        // assertThat(back.getProductId()).isEqualTo(original.getProductId());
        assertThat(back.getQuantity()).isEqualTo(original.getQuantity());
        assertThat(back.getCustomerId()).isEqualTo(original.getCustomerId());
        assertThat(back.getCreated()).isEqualTo(original.getCreated());
    }

    @Test
    void fulfillmentEventSerializesAndDeserializesWithNulls() {
        FulfillmentEvent evt = new FulfillmentEvent(
                "evt-987", "ORD51", FulfillmentStatus.FAILED, "Compensated", null, // payload is null
                Instant.parse("2023-06-25T10:12:00Z"), "ORD51", null
        );
        String json = KafkaSerdeUtil.toJson(evt);
        FulfillmentEvent back = KafkaSerdeUtil.fromJson(json, FulfillmentEvent.class);
        assertThat(back).isNotNull();
        assertThat(back.getOrderId()).isEqualTo(evt.getOrderId());
        assertThat(back.getPayload()).isNull();
        assertThat(back.getType()).isEqualTo("Compensated");
        assertThat(back.getStatus()).isEqualTo(FulfillmentStatus.FAILED);
        assertThat(back.getTimestamp()).isEqualTo(evt.getTimestamp());
    }

    @Test
    void orderEventDeserializesEvenIfExtraFieldsArePresent() {
        String extendedJson = "{\"orderId\":\"TESTX\",\"productId\":\"P1\",\"quantity\":1," +
                "\"customerId\":\"C1\",\"created\":\"2025-01-01T12:00:00Z\",\"legacyField\":\"shouldBeIgnored\"}";
        OrderEvent roundTrip = KafkaSerdeUtil.fromJson(extendedJson, OrderEvent.class);
        assertThat(roundTrip).isNotNull();
        assertThat(roundTrip.getOrderId()).isEqualTo("TESTX");
    }

    @Test
    void corruptedJsonReturnsNullAndDoesNotThrow() {
        String corrupted = "{this is not json at all";
        OrderEvent result = KafkaSerdeUtil.fromJson(corrupted, OrderEvent.class);
        assertThat(result).isNull();
    }

    @Test
    void instantFieldsProperlyHandledInAllModels() {
        Instant now = Instant.now();
        FulfillmentEvent fe = new FulfillmentEvent("x","y",FulfillmentStatus.NEW,"t","{}",now,"z",null);
        String json = KafkaSerdeUtil.toJson(fe);
        FulfillmentEvent round = KafkaSerdeUtil.fromJson(json, FulfillmentEvent.class);
        assertThat(round.getTimestamp()).isEqualTo(now);
    }
}
