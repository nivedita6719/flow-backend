package com.flow.service;

import com.flow.model.WorkflowJobPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
@RequiredArgsConstructor
@Slf4j
public class RetryQueueService {

    private final RedissonClient redissonClient;
    private final QueueService queueService;

    // Sorted set — score is retry timestamp
    private static final String RETRY_QUEUE = "workflow:runs:retry";

    // Add job to retry queue with delay
    public void scheduleRetry(WorkflowJobPayload payload) {

        long delay = payload.calculateNextDelay();
        long retryAt = System.currentTimeMillis() + delay;

        payload.setAttemptNumber(payload.getAttemptNumber() + 1);
        payload.setNextRetryAt(retryAt);

        RScoredSortedSet<WorkflowJobPayload> retryQueue =
                redissonClient.getScoredSortedSet(RETRY_QUEUE);

        // score = retry timestamp
        retryQueue.add(retryAt, payload);

        log.info(
                "Job scheduled for retry. runId: {}, attempt: {}, retryIn: {}ms",
                payload.getRunId(),
                payload.getAttemptNumber(),
                delay
        );
    }

    // Check retry queue every 5 seconds
    @Scheduled(fixedDelay = 5000)
    public void processRetryQueue() {

        RScoredSortedSet<WorkflowJobPayload> retryQueue =
                redissonClient.getScoredSortedSet(RETRY_QUEUE);

        if (retryQueue.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();

        // Get jobs whose retry time has passed
        Collection<WorkflowJobPayload> readyJobs =
                retryQueue.valueRange(
                        0,
                        true,
                        now,
                        true
                );

        if (readyJobs.isEmpty()) {
            return;
        }

        log.info(
                "Moving {} retry jobs back to main queue",
                readyJobs.size()
        );

        for (WorkflowJobPayload job : readyJobs) {

            // Remove from retry queue
            retryQueue.remove(job);

            // Push back to main queue
            queueService.pushJob(job);

            log.info(
                    "Retry job moved to main queue. runId: {}, attempt: {}",
                    job.getRunId(),
                    job.getAttemptNumber()
            );
        }
    }

    public long getRetryQueueSize() {

        return redissonClient
                .getScoredSortedSet(RETRY_QUEUE)
                .size();
    }
}