
package com.flow.service;

import com.flow.model.Workflow;
import com.flow.model.WorkflowJobPayload;
import com.flow.model.WorkflowRun;
import com.flow.repository.WorkflowRepository;
import com.flow.repository.WorkflowRunRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CronTriggerService {

    private final WorkflowRepository workflowRepository;
    private final WorkflowRunRepository workflowRunRepository;

    // Replace direct execution with queue-based execution
    private final QueueService queueService;

    // Runs every 60 seconds
    @Scheduled(fixedRate = 60000)
    public void checkAndTriggerCronWorkflows() {

        log.debug(
                "Checking cron workflows at: {}",
                LocalDateTime.now()
        );

        // Get all published CRON workflows
        List<Workflow> cronWorkflows =
                workflowRepository.findByStatusAndTriggerType(
                        Workflow.WorkflowStatus.PUBLISHED,
                        Workflow.TriggerType.CRON
                );

        if (cronWorkflows.isEmpty()) {

            log.debug("No cron workflows found");
            return;
        }

        log.info(
                "Found {} cron workflows to check",
                cronWorkflows.size()
        );

        for (Workflow workflow : cronWorkflows) {

            try {

                if (shouldRunNow(workflow)) {

                    log.info(
                            "Triggering cron workflow: {} ({})",
                            workflow.getName(),
                            workflow.getId()
                    );

                    triggerWorkflow(workflow);
                }

            } catch (Exception e) {

                log.error(
                        "Error processing cron workflow {}: {}",
                        workflow.getId(),
                        e.getMessage()
                );
            }
        }
    }

    private boolean shouldRunNow(Workflow workflow) {

        String cronString =
                workflow.getCronString();

        if (cronString == null
                || cronString.isBlank()) {

            log.warn(
                    "Workflow {} has no cron string",
                    workflow.getId()
            );

            return false;
        }

        try {

            // Parse cron expression
            CronExpression cronExpression =
                    CronExpression.parse(cronString);

            // Get latest workflow runs
            List<WorkflowRun> recentRuns =
                    workflowRunRepository
                            .findByWorkflowIdOrderByStartedAtDesc(
                                    workflow.getId()
                            );

            LocalDateTime now =
                    LocalDateTime.now();

            // Never executed before
            if (recentRuns.isEmpty()) {
                return true;
            }

            // Last execution time
            LocalDateTime lastRunTime =
                    recentRuns.get(0).getStartedAt();

            // Calculate next execution time
            LocalDateTime nextScheduledTime =
                    cronExpression.next(lastRunTime);

            if (nextScheduledTime == null) {
                return false;
            }

            // Run if current time reached
            return !nextScheduledTime.isAfter(now);

        } catch (Exception e) {

            log.error(
                    "Invalid cron expression for workflow {}: {}",
                    workflow.getId(),
                    e.getMessage()
            );

            return false;
        }
    }

    private void triggerWorkflow(
            Workflow workflow
    ) {

        // Build trigger payload
        Map<String, Object> triggerPayload =
                new HashMap<>();

        triggerPayload.put(
                "triggerType",
                "CRON"
        );

        triggerPayload.put(
                "scheduledAt",
                LocalDateTime.now().toString()
        );

        triggerPayload.put(
                "workflowId",
                workflow.getId()
        );

        // Create workflow run record
        WorkflowRun run =
                WorkflowRun.builder()
                        .workflow(workflow)
                        .status(
                                WorkflowRun.RunStatus.PENDING
                        )
                        .triggerType(
                                Workflow.TriggerType.CRON
                        )
                        .triggerPayload(
                                triggerPayload.toString()
                        )
                        .build();

        WorkflowRun savedRun =
                workflowRunRepository.save(run);

        // Build queue job payload

        WorkflowJobPayload jobPayload = WorkflowJobPayload.builder()
                .runId(savedRun.getId())
                .workflowId(workflow.getId())
                .triggerPayload(triggerPayload)
                .attemptNumber(1)
                .maxAttempts(3)      // ← Add this
                .nextRetryAt(0)      // ← Add this
                .build();

        // Push to Redis queue
        queueService.pushJob(jobPayload);

        log.info(
                "Cron job queued. runId: {}",
                savedRun.getId()
        );
    }
}