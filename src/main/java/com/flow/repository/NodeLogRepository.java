package com.flow.repository;

import com.flow.model.NodeLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NodeLogRepository
        extends JpaRepository<NodeLog, String> {

    // All logs for a run — ordered by execution time
    List<NodeLog> findByWorkflowRunIdOrderByExecutedAtAsc(
            String runId
    );

    // Logs for specific node type — useful for debugging
    List<NodeLog> findByWorkflowRunIdAndNodeType(
            String runId,
            String nodeType
    );

    // Failed nodes only
    List<NodeLog> findByWorkflowRunIdAndStatus(
            String runId,
            NodeLog.NodeStatus status
    );

    // Count logs per run — for stats
    long countByWorkflowRunId(String runId);
}
