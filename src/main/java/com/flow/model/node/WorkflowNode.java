package com.flow.model.node;

import lombok.Data;
import java.util.Map;

@Data
public class WorkflowNode {
    private String id;
    private String type;
    private Map<String, Object> config;
    private String next;
}
