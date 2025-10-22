# WES Orchestration Engine - Usage Guide

## Quick Start for Developers

This guide explains how to use the implemented domain logic for orchestrating workflows with saga patterns.

---

## 1. Creating and Starting a Workflow

```java
// Step 1: Create workflow instance
WorkflowInstance workflow = WorkflowInstance.builder()
    .id("wf-001")
    .workflowDefinitionId("order-fulfillment-v1")
    .workflowName("Order Fulfillment")
    .workflowType(WorkflowType.ORDER_FULFILLMENT)
    .status(WorkflowStatus.PENDING)
    .priority(WorkflowPriority.HIGH)
    .executionContext(new ExecutionContext())
    .steps(createWorkflowSteps())
    .executedSteps(new ArrayList<>())
    .compensatedSteps(new ArrayList<>())
    .errors(new ArrayList<>())
    .maxRetries(3)
    .inputParameters(Map.of("orderId", "ORD-12345"))
    .triggeredBy("system")
    .correlationId("corr-001")
    .build();

// Step 2: Start the saga
sagaCoordinatorService.startSaga(workflow);

// Domain events emitted:
// - WorkflowStartedEvent
```

---

## 2. Executing Workflow Steps

```java
// Step 1: Start step execution
workflow.startStepExecution("step-1");

// Step 2: Execute business logic in the target service
// ... call inventory service, robotics service, etc.

// Step 3: Mark step as completed
StepResult result = StepResult.success(
    "step-1",
    Map.of("inventoryReserved", true, "quantity", 10),
    1500L // execution time in ms
);

workflowExecutionService.executeStep(workflow, "step-1", result);

// Domain events emitted:
// - WorkflowStepExecutedEvent
```

---

## 3. Handling Step Failures with Retry

```java
// Step 1: Create error
WorkflowError error = WorkflowError.create(
    "step-2",
    "inventory-service",
    "INVENTORY_UNAVAILABLE",
    "Insufficient inventory for product SKU-12345",
    WorkflowError.ErrorType.BUSINESS_RULE_VIOLATION.name(),
    true // recoverable
);

// Step 2: Handle failure (automatic retry if applicable)
workflowExecutionService.handleStepFailure(workflow, "step-2", error);

// The service will:
// 1. Check if step can be retried (max 3 times)
// 2. Calculate exponential backoff delay (1s, 2s, 4s)
// 3. Mark step for retry OR trigger saga failure

// Domain events emitted:
// - WorkflowStepFailedEvent
```

---

## 4. Saga Compensation (Rollback)

When a step fails and cannot be retried, the saga coordinator will automatically
trigger backward recovery (compensation) in reverse order.

```java
// Automatic compensation trigger on unrecoverable failure
WorkflowError criticalError = WorkflowError.create(
    "step-3",
    "robotics-service",
    "ROBOT_UNAVAILABLE",
    "All robots are offline",
    WorkflowError.ErrorType.SERVICE_UNAVAILABLE.name(),
    false // NOT recoverable
);

// This will automatically:
// 1. Fail the workflow
// 2. Trigger compensation
sagaCoordinatorService.failSaga(workflow, criticalError);

// Compensation executes in REVERSE order:
// - Compensate step-2 (if completed)
// - Compensate step-1 (if completed)

// For each completed step:
for (StepExecution step : workflow.getStepsRequiringCompensation()) {
    CompensationAction action = sagaCoordinatorService.getCompensationAction(step);

    // Execute compensation via service integration adapter
    // ... call service to reverse the operation

    workflow.markStepCompensated(step.getStepId());
}

// Finally, complete compensation
workflow.completeCompensation();

// Domain events emitted:
// - WorkflowFailedEvent
// - WorkflowCompensationStartedEvent
// - WorkflowCompensationCompletedEvent
```

---

## 5. Configuring Compensation Actions

When creating workflow steps, define how to compensate (rollback) each step:

```java
// Example: Reserve Inventory Step
StepExecution reserveInventoryStep = StepExecution.builder()
    .stepId("reserve-inventory")
    .workflowInstanceId(workflowId)
    .stepName("Reserve Inventory")
    .serviceName("inventory-service")
    .operation("reserveInventory")
    .executionOrder(1)
    .inputData(Map.of("sku", "SKU-12345", "quantity", 10))
    .timeoutMs(5000L)
    .retryPolicy(RetryPolicy.defaultPolicy())
    .compensationAction(CompensationAction.reverseOperation(
        "reserve-inventory",
        "inventory-service",
        "releaseInventory",
        Map.of("reservationId", "res-12345")
    ))
    .build();

// Compensation strategies:
// 1. REVERSE_OPERATION - Call opposite operation (reserve → release)
// 2. DELETE_CREATED - Delete created resources
// 3. RESTORE_STATE - Restore previous state from snapshot
// 4. CUSTOM - Custom compensation logic
```

---

## 6. Waveless Processing (Continuous Order Flow)

Enable waveless processing for high-priority order fulfillment:

```java
// Step 1: Check if workflow can transition to waveless
if (workflow.canTransitionToWaveless()) {
    // Step 2: Enable waveless processing
    wavelessProcessingService.enableWaveless(workflow);

    // This sets:
    // - Batch size: 10 orders
    // - Processing interval: 1 second
    // - Priority-based processing: HIGH > NORMAL > LOW
}

// Step 3: Create dynamic batch
List<WorkflowInstance> pendingWorkflows = // ... get pending workflows
List<WorkflowInstance> batch = wavelessProcessingService.createDynamicBatch(
    pendingWorkflows,
    10 // batch size
);

// Batch will be sorted by priority:
// 1. All HIGH priority workflows first
// 2. Then NORMAL priority
// 3. Finally LOW priority

// Domain events emitted:
// - WavelessProcessingEnabledEvent
```

---

## 7. Load Balancing

Monitor system load and rebalance workload:

```java
// Step 1: Monitor system load
Map<String, SystemLoad> serviceLoads = new HashMap<>();

serviceLoads.put("inventory-service", SystemLoad.create(
    "inv-01", "inventory-service", workflowId
));
serviceLoads.put("robotics-service", SystemLoad.create(
    "robot-01", "robotics-service", workflowId
));

// Step 2: Update metrics
LoadMetrics inventoryMetrics = LoadMetrics.snapshot(
    "inv-01",
    "inventory-service",
    75.0, // CPU usage %
    60.0, // Memory usage %
    50,   // Active requests
    100,  // Queue depth
    250L, // Avg response time ms
    0.05  // Error rate (5%)
);

loadBalancingService.updateLoadMetrics(
    serviceLoads.get("inventory-service"),
    inventoryMetrics
);

// Step 3: Check if rebalancing needed
if (loadBalancingService.needsRebalancing(serviceLoads)) {
    // Calculate rebalancing strategy
    Map<String, Double> targetLoads =
        loadBalancingService.calculateRebalancingStrategy(serviceLoads);

    // Apply rebalancing...
}

// Step 4: Select service for new workflow
Optional<String> selectedService =
    loadBalancingService.selectServiceForWorkflow(serviceLoads);

if (selectedService.isPresent()) {
    // Route workflow to selected service
}

// Domain events emitted:
// - SystemLoadRebalancedEvent
```

---

## 8. Workflow Control Operations

### Pause Workflow
```java
workflowExecutionService.pauseExecution(workflow);
// Domain event: WorkflowPausedEvent
```

### Resume Workflow
```java
workflowExecutionService.resumeExecution(workflow);
// Domain event: WorkflowResumedEvent
```

### Cancel Workflow
```java
workflowExecutionService.cancelExecution(workflow, "Cancelled by user");
// Domain event: WorkflowCancelledEvent
```

### Retry Workflow
```java
if (workflow.getRetryCount() < workflow.getMaxRetries()) {
    workflow.retry();
    // Domain event: WorkflowRetryEvent
}
```

---

## 9. Monitoring Workflow Progress

```java
// Get progress percentage (0-100)
double progress = workflow.getProgressPercentage();

// Check if workflow is active
boolean isActive = workflow.isActive();

// Check if workflow is terminal
boolean isTerminal = workflow.isTerminal();

// Check if workflow timed out
boolean timedOut = workflow.hasTimedOut(300000L); // 5 minutes

// Get current system load
SystemLoad load = workflow.calculateSystemLoad();

// Get compensation progress
double compensationProgress =
    sagaCoordinatorService.calculateCompensationProgress(workflow);
```

---

## 10. Workflow Lifecycle States

```
PENDING ─────► EXECUTING ─────► COMPLETED
                   │
                   ├─────► PAUSED ─────► EXECUTING
                   │
                   ├─────► FAILED ─────► COMPENSATING ─────► COMPENSATED
                   │
                   └─────► CANCELLED
```

**State Transitions:**
- PENDING → EXECUTING: workflow.start()
- EXECUTING → COMPLETED: workflow.complete()
- EXECUTING → PAUSED: workflow.pause()
- PAUSED → EXECUTING: workflow.resume()
- EXECUTING → FAILED: workflow.fail(error)
- FAILED → COMPENSATING: workflow.compensate()
- COMPENSATING → COMPENSATED: workflow.completeCompensation()
- EXECUTING → CANCELLED: workflow.cancel(reason)

---

## 11. Retry Policy Configuration

```java
// Default policy (3 retries, 1s initial, 2.0 multiplier)
RetryPolicy defaultPolicy = RetryPolicy.defaultPolicy();

// Aggressive policy (5 retries, faster)
RetryPolicy aggressivePolicy = RetryPolicy.aggressivePolicy();

// Conservative policy (2 retries, slower)
RetryPolicy conservativePolicy = RetryPolicy.conservativePolicy();

// Custom policy
RetryPolicy customPolicy = RetryPolicy.builder()
    .maxRetries(5)
    .initialDelayMs(2000L)
    .maxDelayMs(30000L)
    .backoffMultiplier(3.0)
    .exponentialBackoff(true)
    .build();

// Calculate delay for retry attempt
long delay = customPolicy.calculateDelay(2); // 2nd retry
// Result: 2000 * 3^2 = 18000ms
```

---

## 12. Complete Example: Order Fulfillment Saga

```java
public class OrderFulfillmentExample {

    @Autowired
    private SagaCoordinatorService sagaCoordinator;

    @Autowired
    private WorkflowExecutionService workflowExecution;

    public void executeOrderFulfillment(String orderId) {
        // 1. Create workflow with steps
        WorkflowInstance workflow = createOrderFulfillmentWorkflow(orderId);

        // 2. Start saga
        sagaCoordinator.startSaga(workflow);

        try {
            // 3. Execute Step 1: Reserve Inventory
            workflow.startStepExecution("reserve-inventory");
            StepResult inventoryResult = inventoryService.reserve(orderId);
            workflowExecution.executeStep(workflow, "reserve-inventory", inventoryResult);

            // 4. Execute Step 2: Assign Robot
            workflow.startStepExecution("assign-robot");
            StepResult robotResult = roboticsService.assign(orderId);
            workflowExecution.executeStep(workflow, "assign-robot", robotResult);

            // 5. Execute Step 3: Pick Items
            workflow.startStepExecution("pick-items");
            StepResult pickResult = warehouseService.pick(orderId);
            workflowExecution.executeStep(workflow, "pick-items", pickResult);

            // 6. Execute Step 4: Pack Items
            workflow.startStepExecution("pack-items");
            StepResult packResult = packingService.pack(orderId);
            workflowExecution.executeStep(workflow, "pack-items", packResult);

            // 7. Complete saga
            sagaCoordinator.completeSaga(workflow);

        } catch (Exception e) {
            // Handle failure
            WorkflowError error = WorkflowError.fromException(
                workflow.getCurrentStepId(),
                "service-name",
                e
            );

            // This will trigger automatic compensation if needed
            workflowExecution.handleStepFailure(
                workflow,
                workflow.getCurrentStepId(),
                error
            );
        }
    }

    private WorkflowInstance createOrderFulfillmentWorkflow(String orderId) {
        Map<String, StepExecution> steps = new LinkedHashMap<>();

        // Step 1: Reserve Inventory
        steps.put("reserve-inventory", StepExecution.create(
            "reserve-inventory",
            "wf-" + orderId,
            "Reserve Inventory",
            "inventory-service",
            "reserve",
            1,
            Map.of("orderId", orderId),
            5000L
        ));

        // Add compensation
        steps.get("reserve-inventory").setCompensationAction(
            CompensationAction.reverseOperation(
                "reserve-inventory",
                "inventory-service",
                "release",
                Map.of("orderId", orderId)
            )
        );

        // Step 2: Assign Robot
        steps.put("assign-robot", StepExecution.create(
            "assign-robot",
            "wf-" + orderId,
            "Assign Robot",
            "robotics-service",
            "assign",
            2,
            Map.of("orderId", orderId),
            5000L
        ));

        steps.get("assign-robot").setCompensationAction(
            CompensationAction.reverseOperation(
                "assign-robot",
                "robotics-service",
                "unassign",
                Map.of("orderId", orderId)
            )
        );

        // Step 3: Pick Items (similar pattern)
        // Step 4: Pack Items (similar pattern)

        return WorkflowInstance.builder()
            .id("wf-" + orderId)
            .workflowType(WorkflowType.ORDER_FULFILLMENT)
            .status(WorkflowStatus.PENDING)
            .priority(WorkflowPriority.HIGH)
            .steps(steps)
            .executedSteps(new ArrayList<>())
            .compensatedSteps(new ArrayList<>())
            .errors(new ArrayList<>())
            .maxRetries(3)
            .inputParameters(Map.of("orderId", orderId))
            .correlationId("ord-" + orderId)
            .build();
    }
}
```

---

## 13. Domain Events

All operations emit domain events that can be consumed for:
- Event sourcing
- Audit trail
- Real-time monitoring
- Analytics
- Integration with other services

**Subscribe to events via Kafka:**
```java
@KafkaListener(topics = "orchestration.workflow.events")
public void handleWorkflowEvent(WorkflowStartedEvent event) {
    // Handle event
    log.info("Workflow started: {}", event.getWorkflowInstanceId());
}
```

---

## 14. Best Practices

1. **Always define compensation actions** for steps that modify state
2. **Use idempotent operations** to allow safe retries
3. **Set appropriate timeouts** for each step (default: 5 seconds)
4. **Monitor system load** to prevent overload
5. **Use HIGH priority** only for urgent orders
6. **Enable waveless processing** for continuous flow
7. **Log correlation IDs** for distributed tracing
8. **Handle all exceptions** gracefully
9. **Test compensation logic** thoroughly
10. **Monitor domain events** for real-time insights

---

## 15. Troubleshooting

### Workflow stuck in EXECUTING
- Check for timeout: `workflow.hasTimedOut(timeout)`
- Verify all steps are progressing
- Check system load for bottlenecks

### Compensation failed
- Check compensation action validity
- Verify target service is available
- Review error logs for specific failures

### High error rate
- Check circuit breaker status
- Verify service health
- Review retry policy configuration

### Load imbalance
- Monitor load metrics
- Trigger manual rebalancing
- Adjust batch sizes

---

## Support

For questions or issues:
1. Check IMPLEMENTATION_STATUS.md for component status
2. Review domain service implementations
3. Check unit tests for examples
4. Contact the platform team

---

**Last Updated:** 2025-10-22
**Version:** 1.0.0
