
package com.flow.controller;

import com.flow.config.SecurityUtils;
import com.flow.dto.ApiResponse;
import com.flow.dto.NodeLogResponse;
import com.flow.dto.PageResponse;
import com.flow.dto.WorkflowRunResponse;
import com.flow.model.WorkflowRun;
import com.flow.service.RunHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(
        name = "Run History",
        description = "View workflow execution history and node-level logs"
)
public class RunHistoryController {

    private final RunHistoryService runHistoryService;

    // All runs for a specific workflow — paginated
    @GetMapping("/api/workflows/{workflowId}/runs")
    @Operation(
            summary = "Get runs for workflow",
            description = "Returns paginated list of all executions for a workflow"
    )
    public ResponseEntity<ApiResponse<PageResponse<WorkflowRunResponse>>>
    getRunsByWorkflow(
            @PathVariable String workflowId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false)
            WorkflowRun.RunStatus status
    ) {

        String userId =
                SecurityUtils.getCurrentUserId();

        PageResponse<WorkflowRunResponse> response =
                runHistoryService.getRunsByWorkflow(
                        workflowId,
                        userId,
                        page,
                        size,
                        status
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Runs fetched",
                        response
                )
        );
    }

    // All runs across all workflows for current user
    @GetMapping("/api/runs")
    @Operation(
            summary = "Get all runs",
            description = "Returns paginated runs across all workflows for current user"
    )
    public ResponseEntity<ApiResponse<PageResponse<WorkflowRunResponse>>>
    getAllRuns(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {

        String userId =
                SecurityUtils.getCurrentUserId();

        PageResponse<WorkflowRunResponse> response =
                runHistoryService.getAllRunsByUser(
                        userId,
                        page,
                        size
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "All runs fetched",
                        response
                )
        );
    }

    // Single run details
    @GetMapping("/api/runs/{runId}")
    @Operation(
            summary = "Get single run",
            description = "Returns details of one execution including node stats"
    )
    public ResponseEntity<ApiResponse<WorkflowRunResponse>>
    getRunById(
            @PathVariable String runId
    ) {

        String userId =
                SecurityUtils.getCurrentUserId();

        WorkflowRunResponse response =
                runHistoryService.getRunById(
                        runId,
                        userId
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Run fetched",
                        response
                )
        );
    }

    // Node logs for a specific run
    @GetMapping("/api/runs/{runId}/logs")
    @Operation(
            summary = "Get node logs",
            description = "Returns detailed execution log for every node in a run. " +
                    "Shows input JSON, output JSON, duration, and status per node."
    )
    public ResponseEntity<ApiResponse<List<NodeLogResponse>>>
    getNodeLogs(
            @PathVariable String runId
    ) {

        String userId =
                SecurityUtils.getCurrentUserId();

        List<NodeLogResponse> logs =
                runHistoryService.getNodeLogs(
                        runId,
                        userId
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Node logs fetched. Total: " + logs.size(),
                        logs
                )
        );
    }

    // Workflow execution statistics
    @GetMapping("/api/workflows/{workflowId}/stats")
    @Operation(
            summary = "Get workflow stats",
            description = "Returns success rate, total runs, and failure count"
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>>
    getStats(
            @PathVariable String workflowId
    ) {

        String userId =
                SecurityUtils.getCurrentUserId();

        Map<String, Object> stats =
                runHistoryService.getWorkflowStats(
                        workflowId,
                        userId
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Stats fetched",
                        stats
                )
        );
    }
}