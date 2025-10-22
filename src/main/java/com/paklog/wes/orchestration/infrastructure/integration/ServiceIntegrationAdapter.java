package com.paklog.wes.orchestration.infrastructure.integration;

import com.paklog.wes.orchestration.application.port.out.ServiceIntegrationPort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Service integration adapter with circuit breaker support
 * Implements ServiceIntegrationPort using RestTemplate with Resilience4j
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceIntegrationAdapter implements ServiceIntegrationPort {

    private final RestTemplate restTemplate;

    @Value("${orchestration.integrations.inventory.url:http://inventory:8080}")
    private String inventoryServiceUrl;

    @Value("${orchestration.integrations.task-execution.url:http://task-execution:8082}")
    private String taskExecutionServiceUrl;

    @Override
    @CircuitBreaker(name = "default", fallbackMethod = "callServiceFallback")
    @Retry(name = "default")
    @TimeLimiter(name = "default")
    public <T> T callService(String serviceName, String endpoint, Object request, Class<T> responseType) {
        String serviceUrl = getServiceUrl(serviceName);
        String url = serviceUrl + endpoint;

        log.debug("Calling service {} at {}", serviceName, url);

        ResponseEntity<T> response = restTemplate.exchange(
            url,
            HttpMethod.POST,
            new HttpEntity<>(request),
            responseType
        );

        return response.getBody();
    }

    @Override
    @CircuitBreaker(name = "default", fallbackMethod = "getFromServiceFallback")
    @Retry(name = "default")
    public <T> T getFromService(String serviceName, String endpoint, Class<T> responseType) {
        String serviceUrl = getServiceUrl(serviceName);
        String url = serviceUrl + endpoint;

        log.debug("GET request to service {} at {}", serviceName, url);

        ResponseEntity<T> response = restTemplate.getForEntity(url, responseType);
        return response.getBody();
    }

    @Override
    @CircuitBreaker(name = "default", fallbackMethod = "postToServiceFallback")
    @Retry(name = "default")
    public <T> T postToService(String serviceName, String endpoint, Object request, Class<T> responseType) {
        String serviceUrl = getServiceUrl(serviceName);
        String url = serviceUrl + endpoint;

        log.debug("POST request to service {} at {}", serviceName, url);

        ResponseEntity<T> response = restTemplate.postForEntity(url, request, responseType);
        return response.getBody();
    }

    @Override
    public boolean isServiceAvailable(String serviceName) {
        // This would check circuit breaker state
        log.debug("Checking availability of service: {}", serviceName);
        return true; // Simplified implementation
    }

    // Fallback methods

    private <T> T callServiceFallback(String serviceName, String endpoint, Object request,
                                      Class<T> responseType, Exception ex) {
        log.error("Circuit breaker fallback for service {}: {}", serviceName, ex.getMessage());
        throw new RuntimeException("Service " + serviceName + " is unavailable", ex);
    }

    private <T> T getFromServiceFallback(String serviceName, String endpoint,
                                         Class<T> responseType, Exception ex) {
        log.error("Circuit breaker fallback for GET {}: {}", serviceName, ex.getMessage());
        throw new RuntimeException("Service " + serviceName + " is unavailable", ex);
    }

    private <T> T postToServiceFallback(String serviceName, String endpoint, Object request,
                                        Class<T> responseType, Exception ex) {
        log.error("Circuit breaker fallback for POST {}: {}", serviceName, ex.getMessage());
        throw new RuntimeException("Service " + serviceName + " is unavailable", ex);
    }

    private String getServiceUrl(String serviceName) {
        return switch (serviceName.toLowerCase()) {
            case "inventory" -> inventoryServiceUrl;
            case "task-execution" -> taskExecutionServiceUrl;
            default -> throw new IllegalArgumentException("Unknown service: " + serviceName);
        };
    }
}
