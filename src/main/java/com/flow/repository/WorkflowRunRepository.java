
package com.flow.repository;

import com.flow.model.WorkflowRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WorkflowRunRepository
        extends JpaRepository<WorkflowRun, String> {

    // Paginated runs for a workflow
    Page<WorkflowRun> findByWorkflowIdOrderByStartedAtDesc(
            String workflowId,
            Pageable pageable
    );

    // Non-paginated — used for cron checks
    List<WorkflowRun> findByWorkflowIdOrderByStartedAtDesc(
            String workflowId
    );

    // Filter workflow runs by status
    Page<WorkflowRun> findByWorkflowIdAndStatusOrderByStartedAtDesc(
            String workflowId,
            WorkflowRun.RunStatus status,
            Pageable pageable
    );

    // Count runs by status
    long countByWorkflowIdAndStatus(
            String workflowId,
            WorkflowRun.RunStatus status
    );

    // Recent runs across all workflows of a user
    @Query(
            "SELECT wr FROM WorkflowRun wr " +
                    "WHERE wr.workflow.user.id = :userId " +
                    "ORDER BY wr.startedAt DESC"
    )
    Page<WorkflowRun> findAllByUserId(
            @Param("userId") String userId,
            Pageable pageable
    );

    // Prevent duplicate webhook execution
    boolean existsByWorkflowIdAndTriggerPayload(
            String workflowId,
            String triggerPayload
    );
}