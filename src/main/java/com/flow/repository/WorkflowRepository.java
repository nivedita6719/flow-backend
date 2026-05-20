package com.flow.repository;

import com.flow.model.Workflow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkflowRepository
        extends JpaRepository<Workflow, String> {

    // Get all workflows of a user
    List<Workflow> findByUserId(String userId);

    // Find all published cron workflows
    List<Workflow> findByStatusAndTriggerType(
            Workflow.WorkflowStatus status,
            Workflow.TriggerType triggerType
    );
}
