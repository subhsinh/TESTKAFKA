package com.example.fulfillmentservice;

@FunctionalInterface
public interface EventSender {
    java.util.concurrent.CompletableFuture<?> send(String topic, String key, String value);
}
