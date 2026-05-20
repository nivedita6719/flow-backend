package com.flow.service.node;

import com.flow.model.node.NodeResult;
import com.flow.model.node.WorkflowNode;
import java.util.Map;

public interface NodeExecutor {

    // Which node type does this executor handle?
    String getNodeType();

    // Execute the node — context has all previous outputs
    NodeResult execute(
            WorkflowNode node,
            Map<String, Object> context
    );
}
