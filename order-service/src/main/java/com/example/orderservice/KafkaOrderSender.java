package com.example.orderservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaOrderSender implements OrderSender {

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    @Autowired
    public KafkaOrderSender(KafkaTemplate<String, OrderEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void send(String topic, String key, OrderEvent orderEvent) {
        kafkaTemplate.send(topic, key, orderEvent);
    }
}
