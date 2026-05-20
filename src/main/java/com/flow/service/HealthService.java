package com.flow.service;

import com.flow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class HealthService {

    private final UserRepository userRepository;
    private final RedissonClient redissonClient;
    private final QueueService queueService;
    private final RetryQueueService retryQueueService;

    // Track server start time
    private final LocalDateTime startTime = LocalDateTime.now();

    public Map<String, Object> getHealth() {

        Map<String, Object> health = new HashMap<>();

        // Overall status — starts as UP
        // Changes to DOWN if any critical component fails
        boolean isHealthy = true;

        // Check database
        Map<String, Object> dbStatus = checkDatabase();
        health.put("database", dbStatus);
        if (!"UP".equals(dbStatus.get("status"))) {
            isHealthy = false;
        }

        // Check Redis
        Map<String, Object> redisStatus = checkRedis();
        health.put("redis", redisStatus);
        if (!"UP".equals(redisStatus.get("status"))) {
            isHealthy = false;
        }

        // Queue metrics
        Map<String, Object> queueMetrics = getQueueMetrics();
        health.put("queue", queueMetrics);

        // System info
        health.put("status", isHealthy ? "UP" : "DOWN");
        health.put("timestamp", LocalDateTime.now().toString());
        health.put("uptime", calculateUptime());
        health.put("version", "1.0.0");

        return health;
    }

    private Map<String, Object> checkDatabase() {
        Map<String, Object> status = new HashMap<>();
        try {
            // Simple query to verify DB connection
            long userCount = userRepository.count();
            status.put("status", "UP");
            status.put("totalUsers", userCount);
        } catch (Exception e) {
            log.error("Database health check failed: {}", e.getMessage());
            status.put("status", "DOWN");
            status.put("error", e.getMessage());
        }
        return status;
    }

    private Map<String, Object> checkRedis() {
        Map<String, Object> status = new HashMap<>();
        try {
            // Ping Redis
            redissonClient.getBucket("health:ping").set("pong");
            String pong = (String) redissonClient
                    .getBucket("health:ping").get();

            if ("pong".equals(pong)) {
                status.put("status", "UP");
            } else {
                status.put("status", "DOWN");
            }
        } catch (Exception e) {
            log.error("Redis health check failed: {}", e.getMessage());
            status.put("status", "DOWN");
            status.put("error", e.getMessage());
        }
        return status;
    }

    private Map<String, Object> getQueueMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        try {
            metrics.put("mainQueueSize", queueService.getQueueSize());
            metrics.put("dlqSize", queueService.getDLQSize());
            metrics.put("retryQueueSize",
                    retryQueueService.getRetryQueueSize());
        } catch (Exception e) {
            metrics.put("error", "Could not fetch queue metrics");
        }
        return metrics;
    }

    private String calculateUptime() {
        LocalDateTime now = LocalDateTime.now();
        long hours = java.time.Duration.between(startTime, now).toHours();
        long minutes = java.time.Duration.between(startTime, now)
                .toMinutesPart();
        return hours + "h " + minutes + "m";
    }
}
