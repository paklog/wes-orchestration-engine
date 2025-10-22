package com.paklog.wes.orchestration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * WES Orchestration Engine - Central orchestration service for Paklog WMS/WES platform
 *
 * This service provides real-time multi-system orchestration, waveless processing,
 * dynamic work routing, and system load balancing across all warehouse operations.
 *
 * @author Paklog Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableKafka
@EnableMongoRepositories
@EnableMongoAuditing
@EnableAsync
@EnableScheduling
public class WesOrchestrationEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(WesOrchestrationEngineApplication.class, args);
    }
}