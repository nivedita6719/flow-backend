package com.flow.model.node;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NodeResult {
    private boolean success;
    private Map<String, Object> output;
    private String errorMessage;
    private long durationMs;

    public static NodeResult success(
            Map<String, Object> output, long durationMs) {
        return NodeResult.builder()
                .success(true)
                .output(output)
                .durationMs(durationMs)
                .build();
    }

    public static NodeResult failure(
            String errorMessage, long durationMs) {
        return NodeResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .durationMs(durationMs)
                .build();
    }
}
