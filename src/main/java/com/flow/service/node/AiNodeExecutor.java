package com.flow.service.node;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flow.model.node.NodeResult;
import com.flow.model.node.WorkflowNode;
import com.flow.service.GeminiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class AiNodeExecutor implements NodeExecutor {

    private final GeminiService geminiService;
    private final ObjectMapper objectMapper;

    @Override
    public String getNodeType() {
        return "AI";
    }

    @Override
    public NodeResult execute(
            WorkflowNode node,
            Map<String, Object> context) {

        long start = System.currentTimeMillis();

        try {
            Map<String, Object> config = node.getConfig();

            // Get prompt template from node config
            String promptTemplate = (String) config.get("prompt");

            if (promptTemplate == null || promptTemplate.isBlank()) {
                return NodeResult.failure(
                        "Prompt is required for AI node",
                        System.currentTimeMillis() - start
                );
            }

            // Replace template variables with actual context values
            String resolvedPrompt = resolvePrompt(promptTemplate, context);

            log.info("AI Node executing with prompt: {}...",
                    resolvedPrompt.substring(0, Math.min(100, resolvedPrompt.length()))
            );

            // Call Gemini API
            String aiResponse = geminiService.generateContent(resolvedPrompt);

            // Build output
            Map<String, Object> output = new HashMap<>();
            output.put("aiResponse", aiResponse);
            output.put("promptUsed", resolvedPrompt);
            output.put("model", "gemini-2.0-flash");

            long duration = System.currentTimeMillis() - start;
            log.info("AI Node completed in {}ms", duration);

            return NodeResult.success(output, duration);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            log.error("AI Node failed: {}", e.getMessage());
            return NodeResult.failure(e.getMessage(), duration);
        }
    }

    // Resolves {{node_1.data.title}} style templates
    private String resolvePrompt(
            String template,
            Map<String, Object> context) {

        try {
            // Convert context to JSON string for easy embedding
            String contextJson = objectMapper.writeValueAsString(context);

            // Replace {{context}} with full context JSON
            if (template.contains("{{context}}")) {
                return template.replace("{{context}}", contextJson);
            }

            // Replace individual placeholders
            for (Map.Entry<String, Object> entry : context.entrySet()) {
                String placeholder = "{{" + entry.getKey() + "}}";
                if (template.contains(placeholder)) {
                    template = template.replace(
                            placeholder,
                            entry.getValue() != null
                                    ? entry.getValue().toString()
                                    : "null"
                    );
                }
            }

            return template;

        } catch (Exception e) {
            log.warn("Template resolution failed, using raw prompt");
            return template;
        }
    }
}
