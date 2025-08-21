package com.example.orderservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Service that sends Order events to the "orders" Kafka topic.
 */
@Service
public class OrderProducer {

    private static final String TOPIC = "orders";

    private final KafkaTemplate<String, Order> kafkaTemplate;

    @Autowired
    public OrderProducer(KafkaTemplate<String, Order> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendOrder(Order order) {
        kafkaTemplate.send(TOPIC, order.getId(), order);
    }
}
