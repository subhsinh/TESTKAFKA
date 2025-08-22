## Order Fulfillment Service Implementation

### Features Implemented

- **Kafka Event-Driven Orchestration**: Handles order placement, inventory allocation, and compensation with a saga pattern using Kafka events.
- **Spring Boot & Java**: Service built as a Spring Boot microservice in Java.
- **Business Logic Layer**: Implements `FulfillmentSagaOrchestrator` as the core orchestrator using in-memory event-store for demo/testing.
- **Domain Events**: Uses POJOs for event contracts (OrderEvent, FulfillmentEvent), with robust JSON serialization using Jackson (`KafkaSerdeUtil`).
- **Compensation/Failure Handling**: Triggers rollback logic if inventory allocation fails, appending events for compensation/rollback to the event log.
- **Unit Test Coverage**: Includes comprehensive unit tests for both happy-path (end-to-end saga) and failure/compensation scenarios. Tests are in `src/test/java/com/example/fulfillmentservice/`.
- **Debug Logging**: Output at each significant saga orchestration and event step to aid in lab learning and troubleshooting.
- **Full Maven Integration**: Build and run tests using Maven; compatible with VSCode Java Test Runner (once IDE config is clean).
- **Resolves Java Instant Serialization Issues**: Registers Jackson JavaTimeModule for proper support of `java.time.Instant` fields in all domain events.
- **Correct Maven/Java package structure**: All files use package naming and directory layout compatible with standard Java project conventions.
- **VSCode Integration**: Project and test root setup for reliable test runner and IDE indexing.

### File Overview

- `src/main/java/com/example/fulfillmentservice/`
  - `FulfillmentSagaOrchestrator.java` — Main saga orchestrator.
  - `KafkaSerdeUtil.java` — Jackson-based JSON utilities for (de)serialization.
  - `EventSender.java`, `FulfillmentInventoryGateway.java`, etc. — Decoupled interfaces and gateways for infrastructure and business logic.
  - `model/` — Event/domain model classes (`OrderEvent`, `FulfillmentEvent`, etc.)
- `src/test/java/com/example/fulfillmentservice/`
  - `FulfillmentSagaOrchestratorTest.java` — Orchestrator end-to-end/unit test (happy-path and compensation).
  - `MinimalEventStoreTest.java` — Tests low-level event store/serialization utility functionality.

### Known Issues & Solutions

- **VSCode "Declared Package" Errors:** If your IDE still reports mismatched packages, follow these steps:
  1. Remove `.classpath`, `.project`, `.vscode`, `.settings` from project root.
  2. Rebuild with `mvn clean compile` from the `fulfillment-service` directory.
  3. Reload VSCode window and/or clean Java Language Server workspace (via Command Palette).
  4. If needed, uninstall and reinstall VSCode Java extensions.

- **Test Runner Integration:** Successfully running tests in VSCode and Maven requires properly detected source/test roots. All test classes should now be visible.
