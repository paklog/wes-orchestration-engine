package com.paklog.wes.orchestration.infrastructure.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

/**
 * Kafka consumer for events from other services
 * Listens to events from inventory, returns, robotics, etc.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceEventConsumer {

    /**
     * Consume inventory events
     */
    @KafkaListener(
        topics = "${orchestration.events.topics.integration-events:wes.integration.events}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeIntegrationEvents(
            @Payload byte[] payload,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) Long offset,
            Acknowledgment acknowledgment) {

        try {
            log.debug("Received integration event from topic {} with key {} at offset {}",
                topic, key, offset);

            // Process the event
            processIntegrationEvent(payload, key);

            // Acknowledge message
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }

        } catch (Exception e) {
            log.error("Error processing integration event from topic {}: {}",
                topic, e.getMessage(), e);
            // In production, you might want to send to DLQ or retry
        }
    }

    private void processIntegrationEvent(byte[] payload, String key) {
        // Parse event and update workflow state accordingly
        // This would integrate with OrchestrationApplicationService
        log.debug("Processing integration event with key: {}", key);
    }
}
