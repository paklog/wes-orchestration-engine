# WES Orchestration Engine

Real-time multi-system orchestration engine for the Paklog WMS/WES platform, providing waveless processing, dynamic work routing, and intelligent load balancing across warehouse operations.

## Overview

The WES Orchestration Engine is the central nervous system of the Paklog warehouse management platform. It orchestrates complex workflows across multiple microservices, manages distributed transactions, handles compensation logic, and enables waveless (continuous) order processing for maximum efficiency.

## Architecture

This service follows Paklog's standard architecture patterns:
- **Hexagonal Architecture** (Ports and Adapters)
- **Domain-Driven Design** (DDD)
- **Event-Driven Architecture** with Apache Kafka
- **CloudEvents** specification for event formatting

### Project Structure

```
wes-orchestration-engine/
├── src/
│   ├── main/
│   │   ├── java/com/paklog/wes/orchestration/
│   │   │   ├── domain/               # Core business logic
│   │   │   │   ├── aggregate/        # WorkflowInstance, WorkflowDefinition
│   │   │   │   ├── entity/           # StepExecution, SystemLoad
│   │   │   │   ├── valueobject/      # WorkflowStatus, ExecutionContext
│   │   │   │   ├── service/          # Domain services
│   │   │   │   ├── repository/       # Repository interfaces (ports)
│   │   │   │   └── event/            # Domain events
│   │   │   ├── application/          # Use cases & orchestration
│   │   │   │   ├── port/
│   │   │   │   │   ├── in/           # Input ports (use cases)
│   │   │   │   │   └── out/          # Output ports
│   │   │   │   ├── service/          # Application services
│   │   │   │   ├── command/          # Commands
│   │   │   │   └── query/            # Queries
│   │   │   └── infrastructure/       # External adapters
│   │   │       ├── persistence/      # MongoDB repositories
│   │   │       ├── messaging/        # Kafka publishers/consumers
│   │   │       ├── web/              # REST controllers
│   │   │       └── config/           # Configuration
│   │   └── resources/
│   │       └── application.yml       # Configuration
│   └── test/                         # Tests
├── k8s/                               # Kubernetes manifests
├── docker-compose.yml                 # Local development
├── Dockerfile                         # Container definition
└── pom.xml                           # Maven configuration
```

## Features

### Core Capabilities

- **🔄 Multi-System Orchestration**: Coordinate operations across 20+ microservices
- **🌊 Waveless Processing**: Continuous order flow without batch waves
- **⚖️ Dynamic Load Balancing**: Intelligent work distribution across systems
- **🔁 Saga Pattern Implementation**: Distributed transaction management with compensation
- **🚦 Circuit Breaker**: Resilient service integration with Resilience4j
- **📊 Real-time Monitoring**: System load and performance metrics
- **🔍 Workflow Tracking**: Complete audit trail of all orchestrations

### Workflow Types Supported

- Order Fulfillment (end-to-end)
- Receiving & Putaway
- Picking & Packing
- Shipping & Loading
- Returns Processing
- Cross-Docking Operations
- Inventory Transfers
- Cycle Counting
- Replenishment
- Value-Added Services

## Technology Stack

- **Java 21** - Programming language
- **Spring Boot 3.2.5** - Application framework
- **MongoDB** - Workflow instance persistence
- **Redis** - Caching and distributed locks
- **Apache Kafka** - Event streaming
- **CloudEvents 2.5.0** - Event format specification
- **Resilience4j** - Fault tolerance
- **Micrometer** - Metrics collection
- **OpenTelemetry** - Distributed tracing

## Getting Started

### Prerequisites

- Java 21+
- Maven 3.8+
- Docker & Docker Compose
- MongoDB 7.0+
- Redis 7.2+
- Apache Kafka 3.5+

### Local Development

1. **Clone the repository**
```bash
git clone https://github.com/paklog/wes-orchestration-engine.git
cd wes-orchestration-engine
```

2. **Start infrastructure services**
```bash
docker-compose up -d mongodb kafka redis
```

3. **Build the application**
```bash
mvn clean install
```

4. **Run the application**
```bash
mvn spring-boot:run
```

5. **Verify the service is running**
```bash
curl http://localhost:8090/actuator/health
```

### Using Docker Compose

```bash
# Start all services including the application
docker-compose up -d

# View logs
docker-compose logs -f wes-orchestration-engine

# Stop all services
docker-compose down
```

## API Documentation

Once running, access the interactive API documentation:
- **Swagger UI**: http://localhost:8090/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8090/v3/api-docs

### Key Endpoints

#### Workflow Management
- `POST /api/v1/workflows` - Start new workflow
- `PUT /api/v1/workflows/{id}/pause` - Pause workflow
- `PUT /api/v1/workflows/{id}/resume` - Resume workflow
- `PUT /api/v1/workflows/{id}/cancel` - Cancel workflow
- `PUT /api/v1/workflows/{id}/retry` - Retry failed workflow
- `PUT /api/v1/workflows/{id}/compensate` - Compensate (rollback) workflow

#### Step Execution
- `POST /api/v1/workflows/{id}/steps/execute` - Execute workflow step

#### Waveless Processing
- `PUT /api/v1/workflows/{id}/waveless` - Transition to waveless mode
- `POST /api/v1/workflows/rebalance` - Rebalance system load

#### Monitoring
- `GET /api/v1/workflows/health` - Orchestration engine health

## Configuration

Key configuration properties in `application.yml`:

```yaml
orchestration:
  workflow:
    default-timeout-ms: 300000        # 5 minutes
    max-retries: 3
    max-concurrent-workflows: 100

  waveless:
    enabled: true
    batch-size: 10
    processing-interval-ms: 1000

  system-load:
    rebalance-enabled: true
    rebalance-interval-ms: 30000      # 30 seconds
    target-utilization: 0.85          # 85%
```

## Event Integration

### Published Events
- `WorkflowStartedEvent`
- `WorkflowStepExecutedEvent`
- `WorkflowCompletedEvent`
- `WorkflowFailedEvent`
- `WorkflowCompensationStartedEvent`
- `SystemLoadRebalancedEvent`

### Consumed Events
The orchestration engine subscribes to events from all integrated services:
- Order events from Order Management
- Inventory events from Inventory Service
- Task events from Task Execution Service
- Pick/Pack events from Warehouse Operations
- And many more...

## Deployment

### Kubernetes Deployment

```bash
# Create namespace
kubectl create namespace paklog-wes

# Apply configurations
kubectl apply -f k8s/deployment.yaml

# Check deployment status
kubectl get pods -n paklog-wes
```

### Production Considerations

- **Scaling**: Horizontal scaling supported via Kubernetes HPA
- **High Availability**: Deploy minimum 3 replicas
- **Resource Requirements**:
  - Memory: 1-2 GB per instance
  - CPU: 0.5-1 core per instance
- **Monitoring**: Prometheus metrics exposed at `/actuator/prometheus`

## Testing

```bash
# Run unit tests
mvn test

# Run integration tests
mvn verify

# Run with coverage
mvn clean verify jacoco:report

# View coverage report
open target/site/jacoco/index.html
```

### Test Coverage Requirements
- Unit Tests: >80%
- Integration Tests: >70%
- Domain Logic: >90%

## Performance

### Benchmarks
- **Throughput**: 10,000 events/second
- **Latency**: p99 < 100ms
- **Concurrent Workflows**: 1,000+
- **Availability**: 99.99%

### Optimization Techniques
- Connection pooling for all integrations
- Redis caching for frequently accessed data
- Async processing with CompletableFuture
- Batch processing for waveless mode

## Monitoring & Observability

### Metrics
- Workflow execution metrics
- System load metrics
- Service integration metrics
- Error rates and latencies

### Health Checks
- `/actuator/health` - Overall health
- `/actuator/health/liveness` - Kubernetes liveness
- `/actuator/health/readiness` - Kubernetes readiness

### Distributed Tracing
OpenTelemetry integration for end-to-end request tracing across services.

## Troubleshooting

### Common Issues

1. **Workflow Stuck in EXECUTING**
   - Check service integrations
   - Review circuit breaker status
   - Examine logs for timeout errors

2. **High Memory Usage**
   - Adjust JVM heap settings
   - Check for workflow accumulation
   - Review cache configuration

3. **Kafka Lag**
   - Increase consumer concurrency
   - Check consumer group status
   - Review message processing time

## Contributing

1. Follow hexagonal architecture principles
2. Maintain domain logic in domain layer
3. Keep infrastructure concerns separate
4. Write comprehensive tests for all changes
5. Document domain concepts using ubiquitous language
6. Follow existing code style and conventions

## Support

For issues and questions:
- Create an issue in GitHub
- Contact the Paklog team
- Check the [documentation](https://paklog.github.io/docs)

## License

Copyright © 2024 Paklog. All rights reserved.

---

**Version**: 1.0.0
**Maintained by**: Paklog Team
**Last Updated**: November 2024