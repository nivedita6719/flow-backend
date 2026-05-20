package com.flow.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flow.dto.ApiResponse;
import com.flow.service.RateLimitService;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws Exception {

        String path = request.getRequestURI();
        String method = request.getMethod();

        Bucket bucket;

        // Different limits for different endpoints
        if (path.startsWith("/api/webhook/")) {
            // Webhook — limit by workflowId in path
            String workflowId = extractWorkflowId(path);
            bucket = rateLimitService.getWebhookBucket(workflowId);

        } else if (path.startsWith("/api/auth/")) {
            // Auth — limit by IP address
            String ip = getClientIp(request);
            bucket = rateLimitService.getAuthBucket(ip);

        } else {
            // All other APIs — limit by userId from header
            String userId = extractUserId(request);
            if (userId == null) {
                // Not authenticated — skip rate limit
                // Security filter will handle auth
                return true;
            }
            bucket = rateLimitService.getApiBucket(userId);
        }

        // Check if request is allowed
        if (rateLimitService.isAllowed(bucket)) {
            // Add rate limit headers to response
            response.setHeader(
                    "X-Rate-Limit-Remaining",
                    String.valueOf(rateLimitService.getRemainingTokens(bucket))
            );
            return true;
        }

        // Rate limit exceeded — return 429
        log.warn("Rate limit exceeded for path: {} from IP: {}",
                path, getClientIp(request));

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ApiResponse<Void> errorResponse = ApiResponse.error(
                "Rate limit exceeded. Please slow down and try again."
        );

        response.getWriter().write(
                objectMapper.writeValueAsString(errorResponse)
        );

        return false;
    }

    private String extractWorkflowId(String path) {
        // Path: /api/webhook/{workflowId}
        String[] parts = path.split("/");
        return parts.length > 3 ? parts[3] : "unknown";
    }

    private String extractUserId(HttpServletRequest request) {
        // Get from SecurityContext if available
        try {
            return SecurityUtils.getCurrentUserId();
        } catch (Exception e) {
            return null;
        }
    }

    private String getClientIp(HttpServletRequest request) {
        // Check X-Forwarded-For header first
        // — proxy/load balancer sets this
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
