package com.flow.dto;

import com.flow.model.Workflow;
import com.flow.model.WorkflowRun;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowRunResponse {

    private String id;
    private String workflowId;
    private String workflowName;
    private WorkflowRun.RunStatus status;
    private Workflow.TriggerType triggerType;
    private String triggerPayload;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Integer durationMs;
    private long totalNodes;
    private long successNodes;
    private long failedNodes;

    public static WorkflowRunResponse from(
            WorkflowRun run,
            long totalNodes,
            long successNodes,
            long failedNodes) {

        return WorkflowRunResponse.builder()
                .id(run.getId())
                .workflowId(run.getWorkflow().getId())
                .workflowName(run.getWorkflow().getName())
                .status(run.getStatus())
                .triggerType(run.getTriggerType())
                .triggerPayload(run.getTriggerPayload())
                .startedAt(run.getStartedAt())
                .endedAt(run.getEndedAt())
                .durationMs(run.getDurationMs())
                .totalNodes(totalNodes)
                .successNodes(successNodes)
                .failedNodes(failedNodes)
                .build();
    }
}
