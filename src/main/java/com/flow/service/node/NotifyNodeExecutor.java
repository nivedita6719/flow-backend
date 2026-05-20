package com.flow.service.node;

import com.flow.model.node.NodeResult;
import com.flow.model.node.WorkflowNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class NotifyNodeExecutor implements NodeExecutor {

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String getNodeType() {
        return "NOTIFY";
    }

    @Override
    public NodeResult execute(
            WorkflowNode node,
            Map<String, Object> context) {

        long start = System.currentTimeMillis();

        try {
            Map<String, Object> config = node.getConfig();
            String type = (String) config.get("type");

            if ("SLACK".equalsIgnoreCase(type)) {
                sendSlackMessage(config, context);
            } else {
                throw new IllegalArgumentException(
                        "Unsupported notify type: " + type
                );
            }

            Map<String, Object> output = new HashMap<>();
            output.put("notified", true);
            output.put("type", type);

            long duration = System.currentTimeMillis() - start;
            return NodeResult.success(output, duration);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            return NodeResult.failure(e.getMessage(), duration);
        }
    }

    private void sendSlackMessage(
            Map<String, Object> config,
            Map<String, Object> context) {

        String webhookUrl = (String) config.get("webhookUrl");
        String message    = (String) config.get("message");

        // Replace template variables — {{node_1.output.data}}
        message = resolveTemplate(message, context);

        Map<String, String> body = new HashMap<>();
        body.put("text", message);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> request =
                new HttpEntity<>(body, headers);

        restTemplate.postForEntity(webhookUrl, request, String.class);
        log.info("Slack notification sent");
    }

    // Replace {{node_1.data.field}} with actual values
    private String resolveTemplate(
            String template,
            Map<String, Object> context) {

        if (template == null) return "";

        // Simple template resolution
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            if (template.contains(placeholder)) {
                template = template.replace(
                        placeholder,
                        entry.getValue().toString()
                );
            }
        }
        return template;
    }
}

