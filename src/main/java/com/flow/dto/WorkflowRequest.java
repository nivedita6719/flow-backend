package com.flow.dto;

import com.flow.model.Workflow;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WorkflowRequest {

    @NotBlank(message = "Workflow name is required")
    private String name;

    private String description;

    @NotBlank(message = "Nodes config is required")
    private String nodesConfig;

    @NotNull(message = "Trigger type is required")
    private Workflow.TriggerType triggerType;

    private String cronString;
}
