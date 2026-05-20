package com.flow.service;

import com.flow.dto.WorkflowRequest;
import com.flow.dto.WorkflowResponse;
import com.flow.exception.AppException;
import com.flow.model.User;
import com.flow.model.Workflow;
import com.flow.repository.UserRepository;
import com.flow.repository.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class WorkflowService {

    private final WorkflowRepository workflowRepository;
    private final UserRepository userRepository;

    // CREATE
    public WorkflowResponse create(
            String userId,
            WorkflowRequest request
    ) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new AppException(
                                "User not found",
                                HttpStatus.NOT_FOUND
                        )
                );

        // Validate CRON expression
        if (request.getTriggerType()
                == Workflow.TriggerType.CRON) {

            validateCronExpression(
                    request.getCronString()
            );
        }

        Workflow workflow = Workflow.builder()
                .user(user)
                .name(request.getName())
                .description(request.getDescription())
                .nodesConfig(request.getNodesConfig())
                .triggerType(request.getTriggerType())
                .cronString(request.getCronString())
                .webhookKey(UUID.randomUUID().toString())
                .status(Workflow.WorkflowStatus.DRAFT)
                .build();

        Workflow saved =
                workflowRepository.save(workflow);

        return WorkflowResponse.from(saved);
    }

    // GET ALL
    public List<WorkflowResponse> getAllByUser(
            String userId
    ) {

        return workflowRepository.findByUserId(userId)
                .stream()
                .map(WorkflowResponse::from)
                .collect(Collectors.toList());
    }

    // GET ONE
    public WorkflowResponse getById(
            String workflowId,
            String userId
    ) {

        Workflow workflow =
                findAndVerifyOwnership(
                        workflowId,
                        userId
                );

        return WorkflowResponse.from(workflow);
    }

    // UPDATE
    public WorkflowResponse update(
            String workflowId,
            String userId,
            WorkflowRequest request
    ) {

        Workflow workflow =
                findAndVerifyOwnership(
                        workflowId,
                        userId
                );

        if (workflow.getStatus()
                == Workflow.WorkflowStatus.PUBLISHED) {

            throw new AppException(
                    "Cannot edit a published workflow. " +
                            "Create a new version instead.",
                    HttpStatus.CONFLICT
            );
        }

        // Validate CRON expression
        if (request.getTriggerType()
                == Workflow.TriggerType.CRON) {

            validateCronExpression(
                    request.getCronString()
            );
        }

        workflow.setName(request.getName());

        workflow.setDescription(
                request.getDescription()
        );

        workflow.setNodesConfig(
                request.getNodesConfig()
        );

        workflow.setTriggerType(
                request.getTriggerType()
        );

        workflow.setCronString(
                request.getCronString()
        );

        Workflow updated =
                workflowRepository.save(workflow);

        return WorkflowResponse.from(updated);
    }

    // RESCHEDULE CRON WORKFLOW
    public WorkflowResponse reschedule(
            String workflowId,
            String userId,
            String newCronString
    ) {

        Workflow workflow =
                findAndVerifyOwnership(
                        workflowId,
                        userId
                );

        // Only CRON workflows can be rescheduled
        if (workflow.getTriggerType()
                != Workflow.TriggerType.CRON) {

            throw new AppException(
                    "Cannot reschedule a non-CRON workflow",
                    HttpStatus.BAD_REQUEST
            );
        }

        // Validate new cron expression
        validateCronExpression(newCronString);

        workflow.setCronString(newCronString);

        Workflow updated =
                workflowRepository.save(workflow);

        log.info(
                "Workflow {} rescheduled to: {}",
                workflowId,
                newCronString
        );

        return WorkflowResponse.from(updated);
    }

    // DELETE
    public void delete(
            String workflowId,
            String userId
    ) {

        Workflow workflow =
                findAndVerifyOwnership(
                        workflowId,
                        userId
                );

        workflowRepository.delete(workflow);
    }

    // PUBLISH
    public WorkflowResponse publish(
            String workflowId,
            String userId
    ) {

        Workflow workflow =
                findAndVerifyOwnership(
                        workflowId,
                        userId
                );

        if (workflow.getStatus()
                == Workflow.WorkflowStatus.PUBLISHED) {

            throw new AppException(
                    "Workflow is already published",
                    HttpStatus.CONFLICT
            );
        }

        if (workflow.getNodesConfig() == null
                || workflow.getNodesConfig().isBlank()) {

            throw new AppException(
                    "Cannot publish a workflow with no nodes",
                    HttpStatus.BAD_REQUEST
            );
        }

        workflow.setStatus(
                Workflow.WorkflowStatus.PUBLISHED
        );

        Workflow published =
                workflowRepository.save(workflow);

        return WorkflowResponse.from(published);
    }

    // PUBLIC wrapper for controller usage
    public Workflow getWorkflowEntity(
            String workflowId,
            String userId
    ) {

        return findAndVerifyOwnership(
                workflowId,
                userId
        );
    }

    // PRIVATE helper
    private Workflow findAndVerifyOwnership(
            String workflowId,
            String userId
    ) {

        Workflow workflow =
                workflowRepository.findById(workflowId)
                        .orElseThrow(() ->
                                new AppException(
                                        "Workflow not found",
                                        HttpStatus.NOT_FOUND
                                )
                        );

        if (!workflow.getUser()
                .getId()
                .equals(userId)) {

            throw new AppException(
                    "Access denied",
                    HttpStatus.FORBIDDEN
            );
        }

        return workflow;
    }

    // Validate cron expression
    private void validateCronExpression(
            String cronString
    ) {

        if (cronString == null
                || cronString.isBlank()) {

            throw new AppException(
                    "Cron string is required for CRON trigger type",
                    HttpStatus.BAD_REQUEST
            );
        }

        try {

            CronExpression.parse(cronString);

        } catch (Exception e) {

            throw new AppException(
                    "Invalid cron expression: "
                            + cronString
                            + ". Example: '0 0 9 * * *' "
                            + "for every day at 9 AM",
                    HttpStatus.BAD_REQUEST
            );
        }
    }
}
