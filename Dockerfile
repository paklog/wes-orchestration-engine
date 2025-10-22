# Multi-stage build for optimized image size
FROM maven:3.9.5-eclipse-temurin-21-alpine AS builder

# Set working directory
WORKDIR /app

# Copy Maven files for dependency resolution
COPY pom.xml .

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

# Add maintainer info
LABEL maintainer="Paklog Team"
LABEL description="WES Orchestration Engine - Central orchestration service for Paklog WMS/WES platform"

# Create application user
RUN addgroup -g 1001 -S paklog && \
    adduser -u 1001 -S paklog -G paklog

# Set working directory
WORKDIR /app

# Install additional packages
RUN apk add --no-cache curl bash

# Copy JAR from builder stage
COPY --from=builder --chown=paklog:paklog /app/target/wes-orchestration-engine-*.jar app.jar

# Copy scripts
COPY --chown=paklog:paklog docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh

# Create directories for logs and temp files
RUN mkdir -p /app/logs /app/temp && \
    chown -R paklog:paklog /app

# Switch to non-root user
USER paklog

# JVM Configuration
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:InitialRAMPercentage=50.0 \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/app/logs/heap-dump.hprof \
    -Djava.security.egd=file:/dev/./urandom \
    -Djava.io.tmpdir=/app/temp"

# Application configuration
ENV SPRING_PROFILES_ACTIVE=docker
ENV SERVER_PORT=8090

# Expose ports
EXPOSE 8090

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8090/actuator/health || exit 1

# Entry point
ENTRYPOINT ["/docker-entrypoint.sh"]

# Default command
CMD ["java", "-jar", "app.jar"]