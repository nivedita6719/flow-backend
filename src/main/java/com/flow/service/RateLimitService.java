package com.flow.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    // Store one bucket per key — key can be userId or IP
    private final ConcurrentHashMap<String, Bucket> buckets =
            new ConcurrentHashMap<>();

    // General API — 100 requests per minute per user
    public Bucket getApiBucket(String userId) {
        return buckets.computeIfAbsent(
                "api:" + userId,
                key -> createBucket(100, Duration.ofMinutes(1))
        );
    }

    // Webhook endpoint — 10 triggers per minute per workflow
    public Bucket getWebhookBucket(String workflowId) {
        return buckets.computeIfAbsent(
                "webhook:" + workflowId,
                key -> createBucket(10, Duration.ofMinutes(1))
        );
    }

    // Auth endpoints — 5 attempts per minute per IP
    // Prevents brute force login attacks
    public Bucket getAuthBucket(String ipAddress) {
        return buckets.computeIfAbsent(
                "auth:" + ipAddress,
                key -> createBucket(5, Duration.ofMinutes(1))
        );
    }

    // Check if request is allowed
    public boolean isAllowed(Bucket bucket) {
        return bucket.tryConsume(1);
    }

    // Create bucket with limit and refill period
    private Bucket createBucket(int capacity, Duration refillPeriod) {
        Bandwidth limit = Bandwidth.classic(
                capacity,
                Refill.greedy(capacity, refillPeriod)
        );
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    // Get remaining tokens — useful for response headers
    public long getRemainingTokens(Bucket bucket) {
        return bucket.getAvailableTokens();
    }
}

