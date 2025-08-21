package com.example.orderservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Exposes a POST /orders endpoint to place new orders.
 */
@RestController
@RequestMapping("/orders")
public class OrderController {

    @Autowired
    private OrderProducer orderProducer;

    @PostMapping
    public ResponseEntity<String> placeOrder(@RequestBody Order order) {
        orderProducer.sendOrder(order);
        return ResponseEntity.ok("Order placed and sent to Kafka topic.");
    }
}
