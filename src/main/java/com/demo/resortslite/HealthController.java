package com.demo.resortslite;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Health check controller for monitoring application status.
 * Provides endpoints for health checks and application information.
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    /**
     * Basic health check endpoint.
     * 
     * @return a map containing the health status
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("application", "ResortsLite");
        response.put("version", "1.0.0");
        return response;
    }

    /**
     * Application information endpoint.
     * 
     * @return a map containing application details
     */
    @GetMapping("/info")
    public Map<String, Object> info() {
        Map<String, Object> response = new HashMap<>();
        response.put("name", "ResortsLite");
        response.put("description", "Resort booking demo — Java 21 / Spring Boot 3.2.x");
        response.put("version", "1.0.0");
        response.put("java.version", System.getProperty("java.version"));
        response.put("spring.boot.version", "3.2.0");
        return response;
    }
}
