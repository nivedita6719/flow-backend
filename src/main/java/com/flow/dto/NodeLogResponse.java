package com.flow.dto;

import com.flow.model.NodeLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NodeLogResponse {

    private String id;
    private String nodeId;
    private String nodeType;
    private String inputJson;
    private String outputJson;
    private NodeLog.NodeStatus status;
    private String errorMessage;
    private Integer durationMs;
    private LocalDateTime executedAt;

    public static NodeLogResponse from(NodeLog log) {
        return NodeLogResponse.builder()
                .id(log.getId())
                .nodeId(log.getNodeId())
                .nodeType(log.getNodeType())
                .inputJson(log.getInputJson())
                .outputJson(log.getOutputJson())
                .status(log.getStatus())
                .errorMessage(log.getErrorMessage())
                .durationMs(log.getDurationMs())
                .executedAt(log.getExecutedAt())
                .build();
    }
}
