package com.flow.service;

import com.flow.dto.NodeLogResponse;
import com.flow.dto.PageResponse;
import com.flow.dto.WorkflowRunResponse;
import com.flow.exception.AppException;
import com.flow.model.NodeLog;
import com.flow.model.Workflow;
import com.flow.model.WorkflowRun;
import com.flow.repository.NodeLogRepository;
import com.flow.repository.WorkflowRepository;
import com.flow.repository.WorkflowRunRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RunHistoryService {

    private final WorkflowRunRepository runRepository;
    private final NodeLogRepository nodeLogRepository;
    private final WorkflowRepository workflowRepository;

    // Get paginated runs for a specific workflow
    public PageResponse<WorkflowRunResponse> getRunsByWorkflow(
            String workflowId,
            String userId,
            int page,
            int size,
            WorkflowRun.RunStatus status) {

        // Verify workflow belongs to user
        Workflow workflow = workflowRepository
                .findById(workflowId)
                .orElseThrow(() -> new AppException(
                        "Workflow not found",
                        HttpStatus.NOT_FOUND
                ));

        if (!workflow.getUser().getId().equals(userId)) {
            throw new AppException(
                    "Access denied",
                    HttpStatus.FORBIDDEN
            );
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<WorkflowRun> runs;

        if (status != null) {
            // Filter by status
            runs = runRepository
                    .findByWorkflowIdAndStatusOrderByStartedAtDesc(
                            workflowId, status, pageable
                    );
        } else {
            // All runs
            runs = runRepository
                    .findByWorkflowIdOrderByStartedAtDesc(
                            workflowId, pageable
                    );
        }

        // Map to response with node stats
        Page<WorkflowRunResponse> responsePage = runs.map(run -> {
            long total = nodeLogRepository.countByWorkflowRunId(run.getId());
            long success = nodeLogRepository
                    .findByWorkflowRunIdAndStatus(
                            run.getId(), NodeLog.NodeStatus.SUCCESS
                    ).size();
            long failed = nodeLogRepository
                    .findByWorkflowRunIdAndStatus(
                            run.getId(), NodeLog.NodeStatus.FAILED
                    ).size();

            return WorkflowRunResponse.from(run, total, success, failed);
        });

        return PageResponse.from(responsePage);
    }

    // Get all runs across all workflows for current user
    public PageResponse<WorkflowRunResponse> getAllRunsByUser(
            String userId,
            int page,
            int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<WorkflowRun> runs =
                runRepository.findAllByUserId(userId, pageable);

        Page<WorkflowRunResponse> responsePage = runs.map(run -> {
            long total = nodeLogRepository
                    .countByWorkflowRunId(run.getId());
            long success = nodeLogRepository
                    .findByWorkflowRunIdAndStatus(
                            run.getId(), NodeLog.NodeStatus.SUCCESS
                    ).size();
            long failed = nodeLogRepository
                    .findByWorkflowRunIdAndStatus(
                            run.getId(), NodeLog.NodeStatus.FAILED
                    ).size();

            return WorkflowRunResponse.from(run, total, success, failed);
        });

        return PageResponse.from(responsePage);
    }

    // Get single run details
    public WorkflowRunResponse getRunById(
            String runId,
            String userId) {

        WorkflowRun run = runRepository
                .findById(runId)
                .orElseThrow(() -> new AppException(
                        "Run not found",
                        HttpStatus.NOT_FOUND
                ));

        // Security check
        if (!run.getWorkflow().getUser().getId().equals(userId)) {
            throw new AppException(
                    "Access denied",
                    HttpStatus.FORBIDDEN
            );
        }

        long total = nodeLogRepository.countByWorkflowRunId(runId);
        long success = nodeLogRepository
                .findByWorkflowRunIdAndStatus(
                        runId, NodeLog.NodeStatus.SUCCESS
                ).size();
        long failed = nodeLogRepository
                .findByWorkflowRunIdAndStatus(
                        runId, NodeLog.NodeStatus.FAILED
                ).size();

        return WorkflowRunResponse.from(run, total, success, failed);
    }

    // Get all node logs for a run
    public List<NodeLogResponse> getNodeLogs(
            String runId,
            String userId) {

        WorkflowRun run = runRepository
                .findById(runId)
                .orElseThrow(() -> new AppException(
                        "Run not found",
                        HttpStatus.NOT_FOUND
                ));

        // Security check
        if (!run.getWorkflow().getUser().getId().equals(userId)) {
            throw new AppException(
                    "Access denied",
                    HttpStatus.FORBIDDEN
            );
        }

        return nodeLogRepository
                .findByWorkflowRunIdOrderByExecutedAtAsc(runId)
                .stream()
                .map(NodeLogResponse::from)
                .collect(Collectors.toList());
    }

    // Get workflow execution stats
    public java.util.Map<String, Object> getWorkflowStats(
            String workflowId,
            String userId) {

        Workflow workflow = workflowRepository
                .findById(workflowId)
                .orElseThrow(() -> new AppException(
                        "Workflow not found",
                        HttpStatus.NOT_FOUND
                ));

        if (!workflow.getUser().getId().equals(userId)) {
            throw new AppException(
                    "Access denied",
                    HttpStatus.FORBIDDEN
            );
        }

        long totalRuns = runRepository
                .countByWorkflowIdAndStatus(
                        workflowId, WorkflowRun.RunStatus.SUCCESS
                ) + runRepository.countByWorkflowIdAndStatus(
                workflowId, WorkflowRun.RunStatus.FAILED
        );

        long successRuns = runRepository
                .countByWorkflowIdAndStatus(
                        workflowId, WorkflowRun.RunStatus.SUCCESS
                );

        long failedRuns = runRepository
                .countByWorkflowIdAndStatus(
                        workflowId, WorkflowRun.RunStatus.FAILED
                );

        double successRate = totalRuns > 0
                ? (double) successRuns / totalRuns * 100
                : 0;

        return java.util.Map.of(
                "totalRuns", totalRuns,
                "successRuns", successRuns,
                "failedRuns", failedRuns,
                "successRate", String.format("%.1f%%", successRate)
        );
    }
}
