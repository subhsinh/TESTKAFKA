package com.example.orderservice;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.kafka.core.KafkaTemplate;

import static org.mockito.Mockito.*;

class OrderProducerTest {

    @Test
    void testSendOrder() {
        // Arrange
        KafkaTemplate<String, Order> kafkaTemplate = Mockito.mock(KafkaTemplate.class);
        OrderProducer orderProducer = new OrderProducer(kafkaTemplate);

        Order order = new Order("id5", "Pen", 5);

        // Act
        orderProducer.sendOrder(order);

        // Assert
        verify(kafkaTemplate, times(1)).send("orders", "id5", order);
    }
}
