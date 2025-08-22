package com.example.fulfillmentservice;

import com.example.fulfillmentservice.model.FulfillmentEvent;
import com.example.fulfillmentservice.model.FulfillmentStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for fulfillment status, event log, and basic diagnostics.
 */
@RestController
@RequestMapping("/fulfillment")
public class FulfillmentController {

    private final FulfillmentSagaOrchestrator orchestrator;

    public FulfillmentController(FulfillmentSagaOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @GetMapping("/{orderId}/status")
    public FulfillmentStatus getStatus(@PathVariable String orderId) {
        return orchestrator.getCurrentStatus(orderId);
    }

    @GetMapping("/{orderId}/events")
    public List<FulfillmentEvent> getEvents(@PathVariable String orderId) {
        return orchestrator.getEventLog(orderId);
    }
}
