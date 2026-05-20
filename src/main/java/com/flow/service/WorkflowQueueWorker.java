
package com.flow.service;

import com.flow.model.Workflow;
import com.flow.model.WorkflowJobPayload;
import com.flow.model.WorkflowRun;
import com.flow.repository.WorkflowRepository;
import com.flow.repository.WorkflowRunRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RQueue;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowQueueWorker {

    private final RedissonClient redissonClient;

    private final WorkflowRepository workflowRepository;

    private final WorkflowRunRepository workflowRunRepository;

    private final NodeExecutionEngine nodeExecutionEngine;

    private final QueueService queueService;

    private final RetryQueueService retryQueueService;

    private static final String WORKFLOW_QUEUE =
            "workflow:runs:queue";

    private static final int MAX_ATTEMPTS = 3;

    // 5 workflows can execute simultaneously
    private final ExecutorService executorService =
            Executors.newFixedThreadPool(5);

    // Poll queue every 2 seconds
    @Scheduled(fixedDelay = 2000)
    public void processQueue() {

        RQueue<WorkflowJobPayload> queue =
                redissonClient.getQueue(
                        WORKFLOW_QUEUE
                );

        // Process max 5 jobs per cycle
        int processed = 0;

        while (processed < 5) {

            // Remove job from queue
            WorkflowJobPayload payload =
                    queue.poll();

            // Queue empty
            if (payload == null) {
                break;
            }

            // Set retry config if not already set
            if (payload.getMaxAttempts() == 0) {

                payload.setMaxAttempts(
                        MAX_ATTEMPTS
                );
            }

            log.info(
                    "Job picked. runId: {}, attempt: {}/{}",
                    payload.getRunId(),
                    payload.getAttemptNumber(),
                    payload.getMaxAttempts()
            );

            // Execute asynchronously
            final WorkflowJobPayload finalPayload =
                    payload;

            executorService.submit(
                    () -> executeJob(finalPayload)
            );

            processed++;
        }
    }

    private void executeJob(
            WorkflowJobPayload payload
    ) {

        try {

            // Load WorkflowRun
            Optional<WorkflowRun> runOpt =
                    workflowRunRepository.findById(
                            payload.getRunId()
                    );

            if (runOpt.isEmpty()) {

                log.error(
                        "WorkflowRun not found: {}",
                        payload.getRunId()
                );

                return;
            }

            WorkflowRun run = runOpt.get();

            // Load Workflow
            Optional<Workflow> workflowOpt =
                    workflowRepository.findById(
                            payload.getWorkflowId()
                    );

            if (workflowOpt.isEmpty()) {

                log.error(
                        "Workflow not found: {}",
                        payload.getWorkflowId()
                );

                return;
            }

            Workflow workflow =
                    workflowOpt.get();

            // Execute workflow
            nodeExecutionEngine.execute(
                    run,
                    workflow,
                    payload.getTriggerPayload()
            );

            log.info(
                    "Job executed successfully. runId: {}",
                    payload.getRunId()
            );

        } catch (Exception e) {

            log.error(
                    "Job execution failed. runId: {}, " +
                            "attempt: {}, error: {}",
                    payload.getRunId(),
                    payload.getAttemptNumber(),
                    e.getMessage()
            );

            handleFailure(
                    payload,
                    e.getMessage()
            );
        }
    }

    private void handleFailure(
            WorkflowJobPayload payload,
            String errorMessage
    ) {

        // Retry allowed
        if (payload.canRetry()) {

            long delay =
                    payload.calculateNextDelay();

            log.warn(
                    "Scheduling retry. runId: {}, " +
                            "nextAttempt: {}, delayMs: {}",
                    payload.getRunId(),
                    payload.getAttemptNumber() + 1,
                    delay
            );

            retryQueueService.scheduleRetry(
                    payload
            );

        } else {

            // Max retries reached
            log.error(
                    "Max attempts reached. " +
                            "Moving to DLQ. runId: {}",
                    payload.getRunId()
            );

            queueService.pushToDLQ(payload);
        }
    }
}