package com.flow.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flow.model.NodeLog;
import com.flow.model.Workflow;
import com.flow.model.WorkflowRun;
import com.flow.model.node.NodeResult;
import com.flow.model.node.WorkflowNode;
import com.flow.repository.NodeLogRepository;
import com.flow.repository.WorkflowRunRepository;
import com.flow.service.node.NodeExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class NodeExecutionEngine {

    private final Map<String, NodeExecutor> executors;
    private final WorkflowRunRepository runRepository;
    private final NodeLogRepository nodeLogRepository;
    private final ObjectMapper objectMapper;

    // Spring automatically injects all NodeExecutor implementations
    // This is why we have getNodeType() — for this map
    public NodeExecutionEngine(
            List<NodeExecutor> nodeExecutors,
            WorkflowRunRepository runRepository,
            NodeLogRepository nodeLogRepository,
            ObjectMapper objectMapper) {

        this.runRepository     = runRepository;
        this.nodeLogRepository = nodeLogRepository;
        this.objectMapper      = objectMapper;

        // Build a map: "HTTP" → HttpNodeExecutor, "CONDITION" → ConditionNodeExecutor
        this.executors = new HashMap<>();
        nodeExecutors.forEach(executor ->
                executors.put(executor.getNodeType(), executor)
        );

        log.info("NodeExecutionEngine initialized with executors: {}",
                executors.keySet());
    }

    public void execute(WorkflowRun run, Workflow workflow,
                        Map<String, Object> triggerPayload) {

        log.info("Starting workflow execution. runId={}, workflowId={}",
                run.getId(), workflow.getId());

        // Mark run as RUNNING
        run.setStatus(WorkflowRun.RunStatus.RUNNING);
        runRepository.save(run);

        try {
            // Parse nodes from JSON
            List<WorkflowNode> nodes = parseNodes(workflow.getNodesConfig());

            // Context carries data between nodes
            Map<String, Object> context = new HashMap<>();
            context.put("trigger", triggerPayload);

            // Execute nodes sequentially
            for (WorkflowNode node : nodes) {
                log.info("Executing node: {} ({})", node.getId(), node.getType());

                NodeExecutor executor = executors.get(node.getType());

                if (executor == null) {
                    throw new IllegalArgumentException(
                            "No executor found for node type: " + node.getType()
                    );
                }

                // Execute the node
                NodeResult result = executor.execute(node, context);

                // Save log — input AND output
                saveNodeLog(run, node, context, result);

                if (!result.isSuccess()) {
                    // Node failed — mark run as failed and stop
                    log.error("Node {} failed: {}",
                            node.getId(), result.getErrorMessage());
                    markRunFailed(run);
                    return;
                }

                // Pass this node's output into context for next node
                context.put(node.getId(), result.getOutput());

                log.info("Node {} completed in {}ms",
                        node.getId(), result.getDurationMs());
            }

            // All nodes succeeded
            markRunSuccess(run);
            log.info("Workflow execution completed. runId={}", run.getId());

        } catch (Exception e) {
            log.error("Workflow execution error: {}", e.getMessage());
            markRunFailed(run);
        }
    }

    private void saveNodeLog(WorkflowRun run, WorkflowNode node,
                             Map<String, Object> context, NodeResult result) {
        try {
            NodeLog nodeLog = NodeLog.builder()
                    .workflowRun(run)
                    .nodeId(node.getId())
                    .nodeType(node.getType())
                    .inputJson(objectMapper.writeValueAsString(context))
                    .outputJson(result.getOutput() != null
                            ? objectMapper.writeValueAsString(result.getOutput())
                            : null)
                    .status(result.isSuccess()
                            ? NodeLog.NodeStatus.SUCCESS
                            : NodeLog.NodeStatus.FAILED)
                    .errorMessage(result.getErrorMessage())
                    .durationMs((int) result.getDurationMs())
                    .build();

            nodeLogRepository.save(nodeLog);

        } catch (Exception e) {
            log.error("Failed to save node log: {}", e.getMessage());
        }
    }

    private void markRunSuccess(WorkflowRun run) {
        run.setStatus(WorkflowRun.RunStatus.SUCCESS);
        run.setEndedAt(LocalDateTime.now());
        run.setDurationMs((int) java.time.Duration.between(
                run.getStartedAt(), run.getEndedAt()).toMillis());
        runRepository.save(run);
    }

    private void markRunFailed(WorkflowRun run) {
        run.setStatus(WorkflowRun.RunStatus.FAILED);
        run.setEndedAt(LocalDateTime.now());
        run.setDurationMs((int) java.time.Duration.between(
                run.getStartedAt(), run.getEndedAt()).toMillis());
        runRepository.save(run);
    }

    private List<WorkflowNode> parseNodes(String nodesConfig) {
        try {
            return objectMapper.readValue(
                    nodesConfig,
                    new TypeReference<List<WorkflowNode>>() {}
            );
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to parse nodes config: " + e.getMessage()
            );
        }
    }
}
