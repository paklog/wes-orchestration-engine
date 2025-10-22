package com.paklog.wes.orchestration.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * MongoDB configuration
 */
@Configuration
@EnableMongoRepositories(basePackages = "com.paklog.wes.orchestration.infrastructure.persistence.mongodb.repository")
@EnableMongoAuditing
public class MongoConfig {
}
