
package com.flow.controller;

import com.flow.config.SecurityUtils;
import com.flow.dto.ApiResponse;
import com.flow.dto.WorkflowRequest;
import com.flow.dto.WorkflowResponse;
import com.flow.model.Workflow;
import com.flow.model.WorkflowRun;
import com.flow.repository.WorkflowRunRepository;
import com.flow.service.NodeExecutionEngine;
import com.flow.service.WorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
@Tag(
        name = "Workflows",
        description = "Create, manage and execute workflows. " +
                "Workflows start as DRAFT and must be " +
                "published before triggering."
)
public class WorkflowController {

    private final WorkflowService workflowService;

    private final NodeExecutionEngine nodeExecutionEngine;

    private final WorkflowRunRepository workflowRunRepository;

    @PostMapping
    @Operation(
            summary = "Create workflow",
            description = "Creates a new workflow in DRAFT status"
    )
    public ResponseEntity<ApiResponse<WorkflowResponse>> create(
            @Valid @RequestBody WorkflowRequest request
    ) {

        String userId =
                SecurityUtils.getCurrentUserId();

        WorkflowResponse response =
                workflowService.create(
                        userId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Workflow created successfully",
                                response
                        )
                );
    }

    @GetMapping
    @Operation(
            summary = "Get all workflows",
            description = "Returns all workflows owned by current user"
    )
    public ResponseEntity<ApiResponse<List<WorkflowResponse>>> getAll() {

        String userId =
                SecurityUtils.getCurrentUserId();

        List<WorkflowResponse> workflows =
                workflowService.getAllByUser(
                        userId
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Workflows fetched",
                        workflows
                )
        );
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get workflow by ID",
            description = "Returns single workflow with full node configuration"
    )
    public ResponseEntity<ApiResponse<WorkflowResponse>> getById(
            @PathVariable String id
    ) {

        String userId =
                SecurityUtils.getCurrentUserId();

        WorkflowResponse response =
                workflowService.getById(
                        id,
                        userId
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Workflow fetched",
                        response
                )
        );
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update workflow",
            description = "Updates workflow nodes. " +
                    "Only allowed when status is DRAFT."
    )
    public ResponseEntity<ApiResponse<WorkflowResponse>> update(
            @PathVariable String id,
            @Valid @RequestBody WorkflowRequest request
    ) {

        String userId =
                SecurityUtils.getCurrentUserId();

        WorkflowResponse response =
                workflowService.update(
                        id,
                        userId,
                        request
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Workflow updated",
                        response
                )
        );
    }

    @PutMapping("/{id}/reschedule")
    @Operation(
            summary = "Reschedule cron workflow",
            description = "Updates cron schedule " +
                    "for a CRON-based workflow"
    )
    public ResponseEntity<ApiResponse<WorkflowResponse>> reschedule(
            @PathVariable String id,
            @RequestParam String cronString
    ) {

        String userId =
                SecurityUtils.getCurrentUserId();

        WorkflowResponse response =
                workflowService.reschedule(
                        id,
                        userId,
                        cronString
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Workflow rescheduled successfully",
                        response
                )
        );
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete workflow",
            description = "Permanently deletes workflow " +
                    "and all its run history"
    )
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable String id
    ) {

        String userId =
                SecurityUtils.getCurrentUserId();

        workflowService.delete(
                id,
                userId
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Workflow deleted",
                        null
                )
        );
    }

    @PostMapping("/{id}/publish")
    @Operation(
            summary = "Publish workflow",
            description = "Changes status from DRAFT to PUBLISHED. " +
                    "Published workflows can be triggered " +
                    "via webhook or cron."
    )
    public ResponseEntity<ApiResponse<WorkflowResponse>> publish(
            @PathVariable String id
    ) {

        String userId =
                SecurityUtils.getCurrentUserId();

        WorkflowResponse response =
                workflowService.publish(
                        id,
                        userId
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Workflow published successfully",
                        response
                )
        );
    }

    @PostMapping("/{id}/test-run")
    @Operation(
            summary = "Test run workflow",
            description = "Executes workflow manually for testing"
    )
    public ResponseEntity<ApiResponse<String>> testRun(
            @PathVariable String id
    ) {

        String userId =
                SecurityUtils.getCurrentUserId();

        Workflow workflow =
                workflowService.getWorkflowEntity(
                        id,
                        userId
                );

        WorkflowRun run =
                WorkflowRun.builder()
                        .workflow(workflow)
                        .status(
                                WorkflowRun.RunStatus.PENDING
                        )
                        .triggerType(
                                Workflow.TriggerType.MANUAL
                        )
                        .triggerPayload("{}")
                        .build();

        WorkflowRun savedRun =
                workflowRunRepository.save(run);

        nodeExecutionEngine.execute(
                savedRun,
                workflow,
                new HashMap<>()
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Test run completed",
                        savedRun.getId()
                )
        );
    }
}