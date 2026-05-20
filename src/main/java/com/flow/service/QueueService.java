package com.flow.service;

import com.flow.model.WorkflowJobPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RQueue;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueueService {

    private final RedissonClient redissonClient;

    // Queue name in Redis
    private static final String WORKFLOW_QUEUE = "workflow:runs:queue";
    private static final String DLQ_QUEUE = "workflow:runs:dlq";

    // Push job to queue
    public void pushJob(WorkflowJobPayload payload) {
        try {
            RQueue<WorkflowJobPayload> queue =
                    redissonClient.getQueue(WORKFLOW_QUEUE);
            queue.add(payload);

            log.info("Job pushed to queue. runId: {}, workflowId: {}",
                    payload.getRunId(), payload.getWorkflowId());

        } catch (Exception e) {
            log.error("Failed to push job to queue: {}", e.getMessage());
            throw new RuntimeException("Queue push failed: " + e.getMessage());
        }
    }

    // Push failed job to Dead Letter Queue
    public void pushToDLQ(WorkflowJobPayload payload) {
        try {
            RQueue<WorkflowJobPayload> dlq =
                    redissonClient.getQueue(DLQ_QUEUE);
            dlq.add(payload);

            log.warn("Job moved to DLQ. runId: {}", payload.getRunId());

        } catch (Exception e) {
            log.error("Failed to push to DLQ: {}", e.getMessage());
        }
    }

    // Get queue size — for health endpoint
    public long getQueueSize() {
        RQueue<WorkflowJobPayload> queue =
                redissonClient.getQueue(WORKFLOW_QUEUE);
        return queue.size();
    }

    // Get DLQ size
    public long getDLQSize() {
        RQueue<WorkflowJobPayload> dlq =
                redissonClient.getQueue(DLQ_QUEUE);
        return dlq.size();
    }
}
