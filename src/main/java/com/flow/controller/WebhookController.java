
package com.flow.controller;

import com.flow.dto.ApiResponse;
import com.flow.service.WebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
@Slf4j
@Tag(
        name = "Webhook Trigger",
        description = "Trigger published workflows via HTTP webhook. " +
                "No JWT required — uses workflow-specific secret key."
)
public class WebhookController {

    private final WebhookService webhookService;

    @PostMapping("/{workflowId}")
    @Operation(
            summary = "Trigger workflow via webhook",
            description = "Triggers a published workflow execution. " +
                    "Returns 202 immediately — execution is async. " +
                    "Use the returned runId to check execution status."
    )
    public ResponseEntity<ApiResponse<Map<String, String>>> trigger(
            @PathVariable String workflowId,
            @RequestParam String key,
            @RequestBody(required = false)
            Map<String, Object> payload
    ) {

        // If request body is empty
        if (payload == null) {

            payload = Map.of();
        }

        String runId =
                webhookService.triggerWorkflow(
                        workflowId,
                        key,
                        payload
                );

        // 202 Accepted = async processing started
        return ResponseEntity
                .accepted()
                .body(
                        ApiResponse.success(
                                "Workflow triggered successfully",
                                Map.of(
                                        "runId",
                                        runId
                                )
                        )
                );
    }
}