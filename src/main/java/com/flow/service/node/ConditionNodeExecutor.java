package com.flow.service.node;

import com.flow.model.node.NodeResult;
import com.flow.model.node.WorkflowNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class ConditionNodeExecutor implements NodeExecutor {

    @Override
    public String getNodeType() {
        return "CONDITION";
    }

    @Override
    public NodeResult execute(
            WorkflowNode node,
            Map<String, Object> context) {

        long start = System.currentTimeMillis();

        try {
            Map<String, Object> config = node.getConfig();

            String field    = (String) config.get("field");
            String operator = (String) config.get("operator");
            String value    = (String) config.get("value");
            String trueNext  = (String) config.get("trueNext");
            String falseNext = (String) config.get("falseNext");

            // Get the actual value from context
            Object actualValue = getValueFromContext(field, context);

            boolean conditionResult = evaluate(
                    actualValue, operator, value
            );

            log.info("Condition: {} {} {} = {}",
                    field, operator, value, conditionResult);

            Map<String, Object> output = new HashMap<>();
            output.put("conditionResult", conditionResult);
            output.put("nextNode", conditionResult ? trueNext : falseNext);

            long duration = System.currentTimeMillis() - start;
            return NodeResult.success(output, duration);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            return NodeResult.failure(e.getMessage(), duration);
        }
    }

    // Supports dot notation — "node_1.data.stargazers_count"
    private Object getValueFromContext(
            String field,
            Map<String, Object> context) {

        String[] parts = field.split("\\.");
        Object current = context;

        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(part);
            } else {
                return null;
            }
        }
        return current;
    }

    private boolean evaluate(
            Object actual,
            String operator,
            String expected) {

        if (actual == null) return false;

        String actualStr = actual.toString();

        return switch (operator.toUpperCase()) {
            case "EQ"       -> actualStr.equals(expected);
            case "NEQ"      -> !actualStr.equals(expected);
            case "CONTAINS" -> actualStr.contains(expected);
            case "GT" -> {
                try {
                    yield Double.parseDouble(actualStr) >
                            Double.parseDouble(expected);
                } catch (NumberFormatException e) {
                    yield false;
                }
            }
            case "LT" -> {
                try {
                    yield Double.parseDouble(actualStr) <
                            Double.parseDouble(expected);
                } catch (NumberFormatException e) {
                    yield false;
                }
            }
            default -> false;
        };
    }
}
