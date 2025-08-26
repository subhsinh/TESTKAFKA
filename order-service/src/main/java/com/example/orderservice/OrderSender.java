package com.example.orderservice;

/**
 * Abstraction for sending orders to Kafka (or stub for tests).
 */
public interface OrderSender {
    void send(String topic, String key, OrderEvent orderEvent);
}
