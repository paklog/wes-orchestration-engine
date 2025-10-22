package com.paklog.wes.orchestration.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paklog.wes.orchestration.application.port.out.PublishEventPort;
import com.paklog.wes.orchestration.domain.event.DomainEvent;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

/**
 * Kafka implementation of PublishEventPort
 * Publishes domain events as CloudEvents to Kafka topics
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowEventPublisher implements PublishEventPort {

    private final KafkaTemplate<String, byte[]> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${orchestration.events.topics.workflow-events}")
    private String workflowEventsTopic;

    @Override
    public void publishEvent(DomainEvent event) {
        publishEvent(workflowEventsTopic, event);
    }

    @Override
    public void publishEvent(String topic, DomainEvent event) {
        try {
            CloudEvent cloudEvent = buildCloudEvent(event);
            byte[] payload = objectMapper.writeValueAsBytes(event);

            CompletableFuture<SendResult<String, byte[]>> future = kafkaTemplate.send(
                topic,
                event.getAggregateId(),
                payload
            );

            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish event {} to topic {}: {}",
                        event.getEventType(), topic, ex.getMessage());
                } else {
                    log.debug("Published event {} to topic {} with offset {}",
                        event.getEventType(), topic, result.getRecordMetadata().offset());
                }
            });

        } catch (Exception e) {
            log.error("Error publishing event {}: {}", event.getEventType(), e.getMessage(), e);
        }
    }

    private CloudEvent buildCloudEvent(DomainEvent event) {
        return CloudEventBuilder.v1()
            .withId(event.getEventId())
            .withType(event.getEventType())
            .withSource(URI.create("/orchestration-engine"))
            .withSubject(event.getAggregateId())
            .withDataContentType("application/json")
            .withTime(event.getOccurredAt().atOffset(java.time.ZoneOffset.UTC))
            .build();
    }
}
