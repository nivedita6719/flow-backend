
package com.flow.controller;

import com.flow.dto.ApiResponse;
import com.flow.model.WorkflowJobPayload;
import com.flow.service.QueueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RQueue;
import org.redisson.api.RedissonClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dlq")
@RequiredArgsConstructor
@Tag(
        name = "Dead Letter Queue",
        description = "Manage failed workflow jobs that exceeded retry attempts"
)
public class DLQController {

    private final RedissonClient redissonClient;
    private final QueueService queueService;

    private static final String DLQ_QUEUE =
            "workflow:runs:dlq";

    // List all failed jobs
    @GetMapping
    @Operation(
            summary = "List DLQ jobs",
            description = "Returns all jobs that failed after maximum retry attempts"
    )
    public ResponseEntity<
            ApiResponse<List<WorkflowJobPayload>>
            > listDLQ() {

        RQueue<WorkflowJobPayload> dlq =
                redissonClient.getQueue(
                        DLQ_QUEUE
                );

        List<WorkflowJobPayload> jobs =
                new ArrayList<>(dlq);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "DLQ jobs fetched. Count: "
                                + jobs.size(),
                        jobs
                )
        );
    }

    // Retry failed job manually
    @PostMapping("/{runId}/retry")
    @Operation(
            summary = "Retry DLQ job",
            description = "Moves a failed job back to main queue for re-execution"
    )
    public ResponseEntity<ApiResponse<String>> retryJob(
            @PathVariable String runId
    ) {

        RQueue<WorkflowJobPayload> dlq =
                redissonClient.getQueue(
                        DLQ_QUEUE
                );

        // Find failed job
        WorkflowJobPayload jobToRetry =
                dlq.stream()
                        .filter(job ->
                                job.getRunId()
                                        .equals(runId)
                        )
                        .findFirst()
                        .orElse(null);

        if (jobToRetry == null) {

            return ResponseEntity
                    .notFound()
                    .build();
        }

        // Remove from DLQ
        dlq.remove(jobToRetry);

        // Reset retry state
        jobToRetry.setAttemptNumber(1);
        jobToRetry.setNextRetryAt(0);

        // Push back to main queue
        queueService.pushJob(jobToRetry);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Job re-queued for retry",
                        runId
                )
        );
    }

    // Queue statistics
    @GetMapping("/stats")
    @Operation(
            summary = "Queue statistics",
            description = "Returns size of main queue, retry queue, and DLQ"
    )
    public ResponseEntity<
            ApiResponse<Map<String, Long>>
            > getStats() {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Queue stats",
                        Map.of(
                                "mainQueueSize",
                                queueService.getQueueSize(),

                                "dlqSize",
                                queueService.getDLQSize()
                        )
                )
        );
    }
}