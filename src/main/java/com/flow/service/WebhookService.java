
package com.flow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flow.exception.AppException;
import com.flow.model.Workflow;
import com.flow.model.WorkflowJobPayload;
import com.flow.model.WorkflowRun;
import com.flow.repository.WorkflowRepository;
import com.flow.repository.WorkflowRunRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {

    private final WorkflowRepository workflowRepository;
    private final WorkflowRunRepository workflowRunRepository;
    private final QueueService queueService;
    private final ObjectMapper objectMapper;

    public String triggerWorkflow(
            String workflowId,
            String webhookKey,
            Map<String, Object> payload) {

        log.info("Webhook received for workflowId: {}", workflowId);

        // Step 1: Find workflow
        Workflow workflow = workflowRepository
                .findById(workflowId)
                .orElseThrow(() -> new AppException(
                        "Workflow not found",
                        HttpStatus.NOT_FOUND
                ));

        // Step 2: Validate webhook key
        if (!workflow.getWebhookKey().equals(webhookKey)) {
            log.warn("Invalid webhook key for workflowId: {}",
                    workflowId);
            throw new AppException(
                    "Invalid webhook key",
                    HttpStatus.UNAUTHORIZED
            );
        }

        // Step 3: Only PUBLISHED workflows
        if (workflow.getStatus() != Workflow.WorkflowStatus.PUBLISHED) {
            throw new AppException(
                    "Workflow is not published",
                    HttpStatus.BAD_REQUEST
            );
        }

        // Step 4: Convert payload to JSON
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            payloadJson = "{}";
        }

        // Step 5: Create run record — status PENDING
        WorkflowRun run = WorkflowRun.builder()
                .workflow(workflow)
                .status(WorkflowRun.RunStatus.PENDING)
                .triggerType(Workflow.TriggerType.WEBHOOK)
                .triggerPayload(payloadJson)
                .build();

        WorkflowRun savedRun = workflowRunRepository.save(run);

        // Step 6: Push to queue — NOT direct execution

        WorkflowJobPayload jobPayload = WorkflowJobPayload.builder()
                .runId(savedRun.getId())
                .workflowId(workflowId)
                .triggerPayload(payload)
                .attemptNumber(1)
                .maxAttempts(3)      // ← Add this
                .nextRetryAt(0)      // ← Add this
                .build();

        queueService.pushJob(jobPayload);

        log.info("Job queued. runId: {}", savedRun.getId());

        // Step 7: Return immediately — 202
        return savedRun.getId();
    }
}