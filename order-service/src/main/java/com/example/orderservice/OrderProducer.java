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

    @Autowired
    private KafkaTemplate<String, Order> kafkaTemplate;

    public void sendOrder(Order order) {
        kafkaTemplate.send(TOPIC, order.getId(), order);
    }
}
