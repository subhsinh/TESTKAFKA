package com.example.orderservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service that sends Order events to the "orders" Kafka topic.
 */
@Service
public class OrderProducer {

    private static final String TOPIC = "orders";

    private final OrderSender orderSender;

    @Autowired
    public OrderProducer(OrderSender orderSender) {
        this.orderSender = orderSender;
    }

    public void sendOrder(Order order) {
        orderSender.send(TOPIC, order.getId(), order);
    }
}
