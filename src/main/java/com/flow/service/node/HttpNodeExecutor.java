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
public class HttpNodeExecutor implements NodeExecutor {

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String getNodeType() {
        return "HTTP";
    }

    @Override
    public NodeResult execute(
            WorkflowNode node,
            Map<String, Object> context) {

        long start = System.currentTimeMillis();

        try {
            Map<String, Object> config = node.getConfig();
            String method = (String) config.get("method");
            String url    = (String) config.get("url");

            log.info("HTTP Node executing: {} {}", method, url);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Object> entity = new HttpEntity<>(
                    config.get("body"),
                    headers
            );

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.valueOf(method),
                    entity,
                    Map.class
            );

            Map<String, Object> output = new HashMap<>();
            output.put("statusCode", response.getStatusCode().value());
            output.put("data", response.getBody());

            long duration = System.currentTimeMillis() - start;
            log.info("HTTP Node completed in {}ms", duration);

            return NodeResult.success(output, duration);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            log.error("HTTP Node failed: {}", e.getMessage());
            return NodeResult.failure(e.getMessage(), duration);
        }
    }
}



