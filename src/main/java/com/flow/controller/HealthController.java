
package com.flow.controller;

import com.flow.dto.ApiResponse;
import com.flow.service.HealthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
@Tag(
        name = "Health",
        description = "System health check — no authentication required"
)
public class HealthController {

    private final HealthService healthService;

    @GetMapping
    @Operation(
            summary = "System health check",
            description = "Returns status of database, Redis, and queue metrics. " +
                    "Returns 200 if all systems UP, 503 if any component DOWN."
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {

        Map<String, Object> healthData =
                healthService.getHealth();

        // Return 503 if any component is DOWN
        boolean isHealthy =
                "UP".equals(healthData.get("status"));

        return isHealthy

                ? ResponseEntity.ok(
                ApiResponse.success(
                        "System healthy",
                        healthData
                )
        )

                : ResponseEntity
                .status(503)
                .body(
                        ApiResponse.error(
                                "System unhealthy"
                        )
                );
    }
}