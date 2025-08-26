package com.example.fulfillmentservice;

import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaEventSender implements EventSender {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    public KafkaEventSender(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public CompletableFuture<?> send(String topic, String key, String value) {
        return kafkaTemplate.send(topic, key, value);
    }
}
