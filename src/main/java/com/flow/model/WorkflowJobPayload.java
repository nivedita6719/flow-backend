
package com.flow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowJobPayload implements Serializable {

    private String runId;

    private String workflowId;

    private Map<String, Object> triggerPayload;

    private int attemptNumber;

    // Retry configuration
    private int maxAttempts;

    // Epoch milliseconds for next retry
    private long nextRetryAt;

    // Check if retry is allowed
    public boolean canRetry() {

        return attemptNumber < maxAttempts;
    }

    // Check if retry time has arrived
    public boolean isReadyToProcess() {

        return System.currentTimeMillis()
                >= nextRetryAt;
    }

    // Exponential backoff delay
    // Attempt 1 -> 2s
    // Attempt 2 -> 4s
    // Attempt 3 -> 8s
    public long calculateNextDelay() {

        return (long) Math.pow(
                2,
                attemptNumber
        ) * 1000;
    }
}