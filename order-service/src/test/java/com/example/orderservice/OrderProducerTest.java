package com.example.orderservice;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class OrderProducerTest {

    @Test
    void testSendOrder() {
        // Arrange
        final boolean[] sendCalled = {false};
        OrderSender sender = new OrderSender() {
            @Override
            public void send(String topic, String key, OrderEvent orderEvent) {
                sendCalled[0] = true;
            }
        };
        OrderProducer orderProducer = new OrderProducer(sender);

        Order order = new Order("id5", "Pen", 5);

        // Act
        orderProducer.sendOrder(order);

        // Assert
        assertTrue(sendCalled[0], "KafkaTemplate.send should have been called");
    }
}
