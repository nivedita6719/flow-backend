package com.flow.dto;

import com.flow.model.Workflow;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowResponse {

    private String id;
    private String name;
    private String description;
    private String nodesConfig;
    private Workflow.WorkflowStatus status;
    private Workflow.TriggerType triggerType;
    private String cronString;
    private String webhookKey;
    private String userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static WorkflowResponse from(Workflow workflow) {
        return WorkflowResponse.builder()
                .id(workflow.getId())
                .name(workflow.getName())
                .description(workflow.getDescription())
                .nodesConfig(workflow.getNodesConfig())
                .status(workflow.getStatus())
                .triggerType(workflow.getTriggerType())
                .cronString(workflow.getCronString())
                .webhookKey(workflow.getWebhookKey())
                .userId(workflow.getUser().getId())
                .createdAt(workflow.getCreatedAt())
                .updatedAt(workflow.getUpdatedAt())
                .build();
    }
}
